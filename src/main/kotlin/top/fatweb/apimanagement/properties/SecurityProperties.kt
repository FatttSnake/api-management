package top.fatweb.apimanagement.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import java.util.concurrent.TimeUnit

/**
 * Security properties
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Validated
data class SecurityProperties(
    /**
     * Key to get authentication from header
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val headerKey: String = "Authorization",
    /**
     * Prefix of token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val tokenPrefix: String = "Bearer ",
    /**
     * Secret to generate token and encrypt plugin secrets
     *
     * There is no usable default: a known secret would let anyone forge a token and read the
     * plugin secrets this value encrypts, so startup fails when it is absent.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val tokenSecret: String = "",
    /**
     * Issuer of token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val tokenIssuer: String = "ApiManagement",
    /**
     * Life of access token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val accessTokenTtl: Long = 2L,
    /**
     * Life util of access token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val accessTokenTtlUnit: TimeUnit = TimeUnit.HOURS,
    /**
     * Life of refresh token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val refreshTokenTtl: Long = 128L,
    /**
     * Whether the cookie can only be transmitted over HTTPS
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val refreshTokenCookieSecure: Boolean = true,
    /**
     * Controls when the cookie is sent with cross-site requests
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val refreshTokenCookieSameSite: String = "Lax",
    /**
     * The URL path for which the cookie is valid
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val refreshTokenCookiePath: String = "/token",
    /**
     * Life util of refresh token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val refreshTokenTtlUnit: TimeUnit = TimeUnit.DAYS,
    /**
     * Prefix of secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val secretKeyPrefix: String = "Basic ",
    /**
     * Whether the gateway runs behind a reverse proxy that sets `X-Real-IP` /
     * `X-Forwarded-For`
     *
     * Off by default: those headers are client-controlled unless a proxy overwrites them, and
     * they feed the API key IP allowlist and the audit log.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val trustProxyHeaders: Boolean = false,
    /**
     * Origins the management console may call from
     *
     * Spring origin patterns are accepted, for example `http://localhost:*`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val allowedOriginPatterns: List<String> = listOf("http://localhost:*", "http://127.0.0.1:*"),
)
