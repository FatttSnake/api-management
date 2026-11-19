package top.fatweb.apimanagement.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import top.fatweb.apimanagement.component.ratelimit.RateLimitDimension
import top.fatweb.apimanagement.component.ratelimit.RateLimitPolicy
import top.fatweb.apimanagement.component.ratelimit.RateLimiter
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.exception.ApiKeyDisabledException
import top.fatweb.apimanagement.exception.ApiKeyExpiredException
import top.fatweb.apimanagement.exception.ApiKeyInvalidException
import top.fatweb.apimanagement.exception.ApiKeyIpNotAllowedException
import top.fatweb.apimanagement.exception.ApiKeySecretMismatchException
import top.fatweb.apimanagement.exception.RequestTooFrequentException
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.util.getRequestIp
import top.fatweb.apimanagement.util.ipMatches
import top.fatweb.apimanagement.util.sha256
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * API key authentication token filter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class ApiKeyAuthenticationTokenFilter(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiKeyService: IApiKeyService,
    private val rateLimitPolicy: RateLimitPolicy,
    private val rateLimiter: RateLimiter,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(this::class.java)

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
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header =
            request.getHeader(serverProperties.security.headerKey) ?: run {
                filterChain.doFilter(request, response)
                return
            }
        val encoded = header.removePrefix(serverProperties.security.secretKeyPrefix).trim()
        val decoded =
            try {
                String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            } catch (_: Exception) {
                throttleOrThrow(request, ApiKeyInvalidException())
            }
        val separator = decoded.indexOf(':')
        if (separator <= 0) {
            throttleOrThrow(request, ApiKeyInvalidException())
        }
        val accessKey = decoded.substring(0, separator)
        val secretKey = decoded.substring(separator + 1)

        val apiKey =
            try {
                loadApiKey(accessKey)
            } catch (ex: ApiKeyInvalidException) {
                throttleOrThrow(request, ex)
            }
        val actualHash = sha256(secretKey).toByteArray(Charsets.UTF_8)
        val expectedHash = apiKey.secretKeyHash?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        if (!MessageDigest.isEqual(actualHash, expectedHash)) {
            throttleOrThrow(request, ApiKeySecretMismatchException())
        }
        checkStatus(apiKey)
        checkIpWhitelist(apiKey, request)

        val principal =
            ApiKeyPrincipal().apply {
                this.keyId = apiKey.id
                this.userId = apiKey.userId
                this.accessKey = apiKey.accessKey
                this.enable = apiKey.enable
                this.permissions =
                    apiKey.permissions
                        ?.split(",")
                        ?.map(String::trim)
                        ?.filter(String::isNotEmpty) ?: emptyList()
                this.rateLimit = apiKey.rateLimit
                this.quota = apiKey.quota
                this.quotaPeriod = apiKey.quotaPeriod
            }

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        filterChain.doFilter(request, response)
    }

    private fun throttleOrThrow(request: HttpServletRequest, failure: RuntimeException): Nothing {
        val limit = rateLimitPolicy.authFailureTotal()
        if (limit <= 0) {
            throw failure
        }

        val ip = getRequestIp(request, serverProperties.security)
        val result = rateLimiter.acquire(RateLimitDimension.AUTH_FAILURE, ip, limit)
        if (result.allowed) {
            throw failure
        }

        log.debug(
            "Auth failure limit hit: ip={} limit={} retryAfter={}",
            ip,
            result.limit,
            result.retryAfterSeconds,
        )
        throw RequestTooFrequentException(
            scope = RateLimitDimension.AUTH_FAILURE.segment,
            limit = result.limit,
            remaining = result.remaining,
            retryAfterSeconds = result.retryAfterSeconds,
        )
    }

    private fun loadApiKey(accessKey: String): ApiKey {
        val cacheKey = cacheKey(accessKey)
        redisProvider.getObject<ApiKey>(cacheKey)?.let { return it }

        val apiKey = apiKeyService.getByAccessKey(accessKey) ?: throw ApiKeyInvalidException()
        redisProvider.setObject(
            cacheKey,
            apiKey,
            SettingsOperator.getValue(ApiSettings::cacheTtlSeconds, 300).toLong(),
            TimeUnit.SECONDS,
        )
        return apiKey
    }

    private fun checkStatus(apiKey: ApiKey) {
        if (apiKey.enable != 1) {
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

        val ip = getRequestIp(request, serverProperties.security)
        if (allowed.none { ipMatches(it, ip) }) {
            throw ApiKeyIpNotAllowedException()
        }
    }

    private fun cacheKey(accessKey: String) = "${serverProperties.security.tokenIssuer}_apikey_$accessKey"
}
