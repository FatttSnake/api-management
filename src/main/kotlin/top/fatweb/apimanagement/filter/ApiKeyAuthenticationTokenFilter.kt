package top.fatweb.apimanagement.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.system.ApiKey
import top.fatweb.apimanagement.entity.system.ApiKeyPrincipal
import top.fatweb.apimanagement.exception.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IApiKeyService
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.util.getRequestIp
import top.fatweb.apimanagement.util.sha256
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * API key authentication token filter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IApiKeyService
 * @see OncePerRequestFilter
 */
@Component
class ApiKeyAuthenticationTokenFilter(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiKeyService: IApiKeyService
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        if (!uri.startsWith("/api/")) {
            return true
        }
        if ("OPTIONS".equals(request.method, true)) {
            return true
        }
        val header = request.getHeader(serverProperties.security.headerKey)
        return header == null || !header.startsWith(serverProperties.security.secretKeyPrefix)
    }

    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val header = request.getHeader(serverProperties.security.headerKey) ?: run {
            filterChain.doFilter(request, response)
            return
        }
        val encoded = header.removePrefix(serverProperties.security.secretKeyPrefix).trim()
        val decoded = try {
            String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        } catch (_: Exception) {
            throw ApiKeyInvalidException()
        }
        val separator = decoded.indexOf(':')
        if (separator <= 0) {
            throw ApiKeyInvalidException()
        }
        val accessKey = decoded.substring(0, separator)
        val secretKey = decoded.substring(separator + 1)

        val apiKey = loadApiKey(accessKey)
        if (sha256(secretKey) != apiKey.secretKeyHash) {
            throw ApiKeySecretMismatchException()
        }
        checkStatus(apiKey)
        checkIpWhitelist(apiKey, request)

        val principal = ApiKeyPrincipal().apply {
            keyId = apiKey.id
            userId = apiKey.userId
            this.accessKey = apiKey.accessKey
            status = apiKey.status
            permissions = apiKey.permissions?.split(",")?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList()
            rateLimit = apiKey.rateLimit
            quota = apiKey.quota
            quotaPeriod = apiKey.quotaPeriod
        }

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        filterChain.doFilter(request, response)
    }

    private fun loadApiKey(accessKey: String): ApiKey {
        val cacheKey = cacheKey(accessKey)
        redisProvider.getObject<ApiKey>(cacheKey)?.let { return it }

        val apiKey = apiKeyService.getByAccessKey(accessKey) ?: throw ApiKeyInvalidException()
        redisProvider.setObject(
            cacheKey,
            apiKey,
            SettingsOperator.getValue(ApiSettings::cacheTtlSeconds, 300).toLong(),
            TimeUnit.SECONDS
        )
        return apiKey
    }

    private fun checkStatus(apiKey: ApiKey) {
        if (apiKey.status != 1) {
            throw ApiKeyDisabledException()
        }
        apiKey.expireTime?.let {
            if (it.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
                throw ApiKeyExpiredException()
            }
        }
    }

    private fun checkIpWhitelist(apiKey: ApiKey, request: HttpServletRequest) {
        val whitelist = apiKey.ipWhitelist ?: return
        val allowed = whitelist.split(",").map(String::trim).filter(String::isNotEmpty)
        if (allowed.isEmpty()) {
            return
        }

        val ip = getRequestIp(request)
        if (allowed.none { ipMatches(it, ip) }) {
            throw ApiKeyIpNotAllowedException()
        }
    }

    private fun ipMatches(pattern: String, ip: String): Boolean {
        if (pattern == ip) {
            return true
        }
        if (!pattern.contains("/")) {
            return false
        }

        return try {
            val (network, prefixText) = pattern.split("/", limit = 2)
            val prefix = prefixText.trim().toInt()
            val mask = if (prefix == 0) 0 else -1 shl (32 - prefix)
            val networkAddress = parseIpV4(network.trim())
            val ipAddress = parseIpV4(ip)
            (networkAddress and mask) == (ipAddress and mask)
        } catch (_: Exception) {
            false
        }
    }

    private fun parseIpV4(ip: String): Int {
        val parts = ip.split(".").map { it.trim().toInt() }
        require(parts.size == 4)
        return (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    }

    private fun cacheKey(accessKey: String) = "${serverProperties.security.tokenIssuer}_apikey_$accessKey"
}
