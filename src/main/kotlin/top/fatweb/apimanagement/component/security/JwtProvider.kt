package top.fatweb.apimanagement.component.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Jwt provider
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 */
@Component
class JwtProvider(
    private val serverProperties: ServerProperties
) {
    private fun getUUID() = UUID.randomUUID().toString().replace("-", "")

    private fun algorithm(): Algorithm = Algorithm.HMAC256(serverProperties.security.tokenSecret)

    /**
     * Generate access token
     *
     * @param subject Data stored in token (JSON format)
     * @return Access token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateAccessToken(
        subject: String
    ) = generateJwt(
        subject = subject,
        ttl = serverProperties.security.accessTokenTtl,
        ttlUnit = serverProperties.security.accessTokenTtlUnit
    )

    /**
     * Generate refresh token
     *
     * @param subject Data stored in token (JSON format)
     * @return Refresh token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateRefreshToken(
        subject: String
    ) = generateJwt(
        subject = subject,
        ttl = serverProperties.security.refreshTokenTtl,
        ttlUnit = serverProperties.security.refreshTokenTtlUnit
    )

    /**
     * Generate jwt token
     *
     * @param subject Data stored in token (JSON format)
     * @param ttl Life of token
     * @param ttlUnit Life util of token
     * @param uuid UUID
     * @return JWT token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimeUnit
     */
    private fun generateJwt(
        subject: String,
        ttl: Long,
        ttlUnit: TimeUnit,
        uuid: String = getUUID()
    ): String? {
        val nowMillis = System.currentTimeMillis()
        val expMillis = nowMillis + ttlUnit.toMillis(ttl)

        return JWT.create().withJWTId(uuid).withSubject(subject).withIssuer(serverProperties.security.tokenIssuer)
            .withIssuedAt(Date(nowMillis)).withExpiresAt(Date(expMillis)).sign(algorithm())
    }

    /**
     * Parse JWT token
     *
     * @param jwt JWT token
     * @return Parsed content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see DecodedJWT
     */
    fun parseJwt(jwt: String): DecodedJWT = JWT.require(algorithm()).build().verify(jwt)
}
