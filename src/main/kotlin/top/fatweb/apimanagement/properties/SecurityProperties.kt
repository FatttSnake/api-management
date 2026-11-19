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
     * Whether the gateway runs behind a reverse proxy or CDN that sets the client IP headers
     * `X-Real-IP` / `X-Forwarded-For`
     *
     * Off by default: those headers are client-controlled unless a proxy overwrites them, and
     * they feed the API key IP allowlist and the audit log.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val trustProxyHeaders: Boolean = false,
    /**
     * Address ranges of the proxies and CDN edges that may set the client IP headers
     *
     * When set, headers are only believed for requests whose peer is in these ranges, and
     * `X-Forwarded-For` is walked from the right past these ranges. When empty, headers are
     * believed from any peer and the first `X-Forwarded-For` entry is taken.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    val trustedProxies: List<String> = emptyList(),
    /**
     * Header the proxy overwrites with the single caller address, for example
     * `CF-Connecting-IP` behind Cloudflare
     *
     * Requires [trustedProxies]: other proxies should leave this unset so a client-supplied
     * copy of the header is not believed.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    val clientIpHeader: String? = null,
    /**
     * Origins the management console may call from
     *
     * Spring origin patterns are accepted, for example `http://localhost:*`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val allowedOriginPatterns: List<String> = listOf("http://localhost:*", "http://127.0.0.1:*"),
) {
    init {
        require(clientIpHeader == null || trustedProxies.isNotEmpty()) {
            "trusted-proxies must be set when client-ip-header is configured"
        }
    }
}
