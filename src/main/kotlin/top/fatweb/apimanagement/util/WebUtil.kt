package top.fatweb.apimanagement.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.properties.ServerProperties

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
 * Get the caller IP
 *
 * Forwarding headers are only trusted when the deployment says a reverse proxy sets them;
 * otherwise any client could choose the address that is checked against an API key's
 * allowlist and written to the audit log.
 *
 * @param request HttpServletRequest object
 * @param trustProxyHeaders whether `X-Real-IP` and `X-Forwarded-For` may be believed
 * @return IP address
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getRequestIp(request: HttpServletRequest, trustProxyHeaders: Boolean = false): String {
    if (!trustProxyHeaders) {
        return request.remoteAddr
    }

    var ip = request.getHeader("X-Real-IP")
    if (!ip.isNullOrBlank() && !"unknown".equals(ip, true)) {
        return ip
    }
    ip = request.getHeader("X-Forwarded-For")
    return if (!ip.isNullOrBlank() && !"unknown".equals(ip, true)) {
        val index = ip.indexOf(",")
        if (index != -1) {
            ip.take(index).trim()
        } else {
            ip.trim()
        }
    } else {
        request.remoteAddr
    }
}
