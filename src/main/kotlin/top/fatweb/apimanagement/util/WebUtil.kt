package top.fatweb.apimanagement.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.properties.SecurityProperties
import top.fatweb.apimanagement.properties.ServerProperties
import java.net.InetAddress

/**
 * Get the user currently calling api
 *
 * @return LoginUser object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getLoginUser(): LoginUser? = if (SecurityContextHolder.getContext().authentication?.principal is String) {
    null
} else {
    SecurityContextHolder.getContext().authentication?.principal as? LoginUser
}

/**
 * Get ID of the user currently calling api
 *
 * @return User ID
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getLoginUserId(): Long? = getLoginUser()?.user?.id

/**
 * Get ID of the user currently calling api, throw when not logged in
 *
 * @return User ID
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see AccessDeniedException
 */
fun getLoginUserIdOrThrow(): Long = getLoginUserId() ?: throw AccessDeniedException("Not logged in")

/**
 * Get username of the user currently calling api
 *
 * @return Username
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getLoginUsername(): String? = getLoginUser()?.user?.username

/**
 * Get the API key principal currently calling api
 *
 * @return ApiKeyPrincipal object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getApiKeyPrincipal(): ApiKeyPrincipal? =
    SecurityContextHolder.getContext().authentication?.principal as? ApiKeyPrincipal

/**
 * Get token of the user currently calling api
 *
 * @param serverProperties Server properties
 * @param tokenWithPrefix Original token
 * @return Token
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getToken(serverProperties: ServerProperties, tokenWithPrefix: String): String =
    tokenWithPrefix.removePrefix(serverProperties.security.tokenPrefix)

/**
 * Get token of the user currently calling api
 *
 * @param serverProperties Server properties
 * @param request Request object
 * @return Token
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getToken(serverProperties: ServerProperties, request: HttpServletRequest): String = getToken(
    serverProperties = serverProperties,
    tokenWithPrefix = request.getHeader(serverProperties.security.headerKey),
)

/**
 * Offline user
 *
 * @param serverProperties Server properties
 * @param redisProvider RedisProvider object
 * @param userIds List of user IDs
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun offlineUser(serverProperties: ServerProperties, redisProvider: RedisProvider, vararg userIds: Long) {
    val keys = HashSet<String>()
    userIds.forEach {
        keys.addAll(redisProvider.keys("${serverProperties.security.tokenIssuer}_token_$it:*"))
        keys.addAll(redisProvider.keys("${serverProperties.security.tokenIssuer}_access_${it}_*:*"))
    }

    redisProvider.delObject(keys)
}

/**
 * Read a forwarding header value, treating blank and `unknown` as absent
 *
 * @return Address or null
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
private fun String?.usableIp(): String? = this?.trim()?.takeIf { it.isNotEmpty() && !"unknown".equals(it, true) }

/**
 * Check whether the value is an IP address without a port or a hostname
 *
 * @param value Address to check
 * @return true when the value is an IPv4 or IPv6 literal
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
fun isIpLiteral(value: String): Boolean = parseIp(value) != null

/**
 * Check whether the IP is inside the address or CIDR range
 *
 * @param range Address or CIDR range
 * @param ip IP address
 * @return true when the IP is inside the range
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
fun ipMatches(range: String, ip: String): Boolean {
    val networkText = range.trim()
    val ipText = ip.trim()
    if (networkText == ipText) {
        return true
    }

    val ipAddress = parseIp(ipText) ?: return false
    if (!networkText.contains("/")) {
        // The same IPv6 address has many textual forms, for example `::1` and `0:0:0:0:0:0:0:1`
        return parseIp(networkText)?.contentEquals(ipAddress) == true
    }

    return try {
        val (network, prefixText) = networkText.split("/", limit = 2)
        val prefix = prefixText.trim().toInt()
        val networkAddress = parseIp(network) ?: return false
        if (ipAddress.size != networkAddress.size || prefix !in 0..networkAddress.size * 8) {
            return false
        }

        val fullBytes = prefix / 8
        if ((0 until fullBytes).any { ipAddress[it] != networkAddress[it] }) {
            return false
        }
        val remainingBits = prefix % 8
        if (remainingBits == 0) {
            return true
        }
        val mask = (0xFF shl (8 - remainingBits)) and 0xFF
        (ipAddress[fullBytes].toInt() and mask) == (networkAddress[fullBytes].toInt() and mask)
    } catch (_: Exception) {
        false
    }
}

/**
 * Parse an IPv4 or IPv6 literal, without resolving hostnames
 *
 * @param value Address to parse
 * @return Address bytes or null
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
private fun parseIp(value: String): ByteArray? {
    val ip = value.trim()
    if (ip.isEmpty()) {
        return null
    }
    if (!ip.contains(':')) {
        val parts = ip.split(".")
        if (parts.size != 4) {
            return null
        }
        val bytes = ByteArray(4)
        parts.forEachIndexed { index, part ->
            val number = part.toIntOrNull() ?: return null
            if (part.isEmpty() || number !in 0..255) {
                return null
            }
            bytes[index] = number.toByte()
        }
        return bytes
    }

    // A value containing ':' is always parsed as an IPv6 literal, never resolved as a hostname
    return try {
        InetAddress.getByName(ip).address
    } catch (_: Exception) {
        null
    }
}

/**
 * Get the caller IP
 *
 * Forwarding headers are only trusted when the deployment says a reverse proxy sets them;
 * otherwise any client could choose the address that is checked against an API key's
 * allowlist and written to the audit log.
 *
 * @param request HttpServletRequest object
 * @param security Security properties
 * @return IP address
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getRequestIp(request: HttpServletRequest, security: SecurityProperties): String {
    if (!security.trustProxyHeaders) {
        return request.remoteAddr
    }

    val peer = request.remoteAddr
    val trustedProxies = security.trustedProxies
    if (trustedProxies.isNotEmpty() && trustedProxies.none { ipMatches(it, peer) }) {
        return peer
    }

    security.clientIpHeader?.let { header ->
        request.getHeader(header).usableIp()?.let { return it }
    }

    val forwarded = request.getHeader("X-Forwarded-For")
        ?.split(",")
        ?.map(String::trim)
        ?.filter { it.isNotEmpty() && !"unknown".equals(it, true) }
        .orEmpty()

    if (trustedProxies.isNotEmpty()) {
        forwarded.asReversed()
            .firstOrNull { hop -> isIpLiteral(hop) && trustedProxies.none { ipMatches(it, hop) } }
            ?.let { return it }
        return request.getHeader("X-Real-IP").usableIp() ?: peer
    }
    request.getHeader("X-Real-IP").usableIp()?.let { return it }

    return forwarded.firstOrNull() ?: peer
}
