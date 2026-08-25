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
     * Secret to generate token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val tokenSecret: String = "ApiManagement",

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
     * @see TimeUnit
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
     * Life util of refresh token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimeUnit
     */
    val refreshTokenTtlUnit: TimeUnit = TimeUnit.DAYS
)
