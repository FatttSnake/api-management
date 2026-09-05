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
 * @see LoginUser
 */
fun getLoginUser(): LoginUser? =
    if (SecurityContextHolder.getContext().authentication?.principal is String) null
    else SecurityContextHolder.getContext().authentication?.principal as? LoginUser

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
fun getLoginUserIdOrThrow(): Long =
    getLoginUserId() ?: throw AccessDeniedException("Not logged in")

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
 * @see ApiKeyPrincipal
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
 * @see ServerProperties
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
 * @see ServerProperties
 * @see HttpServletRequest
 */
fun getToken(serverProperties: ServerProperties, request: HttpServletRequest): String =
    getToken(
        serverProperties = serverProperties,
        tokenWithPrefix = request.getHeader(serverProperties.security.headerKey)
    )

/**
 * Offline user
 *
 * @param serverProperties Server properties
 * @param redisProvider RedisProvider object
 * @param userIds List of user IDs
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 */
fun offlineUser(
    serverProperties: ServerProperties,
    redisProvider: RedisProvider,
    vararg userIds: Long
) {
    val keys = HashSet<String>()
    userIds.forEach {
        keys.addAll(redisProvider.keys("${serverProperties.security.tokenIssuer}_token_${it}:*"))
        keys.addAll(redisProvider.keys("${serverProperties.security.tokenIssuer}_access_${it}_*:*"))
    }

    redisProvider.delObject(keys)
}

/**
 * Get real request IP
 *
 * @param request HttpServletRequest object
 * @return IP address
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see HttpServletRequest
 */
fun getRequestIp(request: HttpServletRequest): String {
    var ip = request.getHeader("X-Real-IP")
    if (!ip.isNullOrBlank() && !"unknown".equals(ip, true)) {
        return ip
    }
    ip = request.getHeader("X-Forwarded-For")
    return if (!ip.isNullOrBlank() && !"unknown".equals(ip, true)) {
        val index = ip.indexOf(",")
        if (index != -1) {
            ip.take(index)
        } else {
            ip
        }
    } else {
        request.remoteAddr
    }
}
