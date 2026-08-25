package top.fatweb.apimanagement.component.security

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.properties.ServerProperties
import java.security.SecureRandom
import java.util.*

/**
 * CSRF token manager
 *
 * Stores each CSRF token under `{issuer}_csrf_{userId}_{refreshToken}:{csrfToken}`,
 * following the same pattern as access tokens. Different sessions have different
 * refresh tokens, so multiple sessions for the same user do not interfere.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RedisProvider
 * @see ServerProperties
 */
@Component
class CsrfTokenManager(
    private val redisProvider: RedisProvider,
    private val serverProperties: ServerProperties
) {
    private val secureRandom = SecureRandom()

    /**
     * Generate a CSRF token and store it under a session-scoped key
     *
     * @param userId User ID to associate the token with
     * @param refreshToken Refresh token of this session (prevents cross-session collision)
     * @return Generated CSRF token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateToken(userId: Long, refreshToken: String): String {
        val token = secureRandom.nextLong().let { UUID(it, it).toString().replace("-", "") }
        redisProvider.setObject(
            key = combinedKey(userId, refreshToken, token),
            value = token,
            timeout = serverProperties.security.refreshTokenTtl,
            timeUnit = serverProperties.security.refreshTokenTtlUnit
        )
        return token
    }

    /**
     * Validate a CSRF token against Redis
     *
     * Constructs the key from userId + refreshToken + csrfToken and checks
     * for existence — the key itself encodes all three values.
     *
     * @param userId User ID that owns the token
     * @param refreshToken Refresh token this CSRF token is bound to
     * @param token CSRF token to validate
     * @return true if valid, false otherwise
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun validateToken(userId: Long, refreshToken: String, token: String): Boolean =
        redisProvider.hasKey(combinedKey(userId, refreshToken, token))

    /**
     * Remove the CSRF token for a specific session
     *
     * Cleans up only the CSRF token tied to the given userId and refreshToken,
     * leaving other sessions of the same user intact.
     *
     * @param userId User ID whose CSRF token to remove
     * @param refreshToken Refresh token of the session to clean up
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun removeToken(userId: Long, refreshToken: String) {
        val pattern = "${serverProperties.security.tokenIssuer}_csrf_${userId}_${refreshToken}:*"
        val keys = redisProvider.keys(pattern)
        if (keys.isNotEmpty()) {
            redisProvider.delObject(keys)
        }
    }

    private fun combinedKey(userId: Long, refreshToken: String, csrfToken: String) =
        "${serverProperties.security.tokenIssuer}_csrf_${userId}_${refreshToken}:${csrfToken}"
}
