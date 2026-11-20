package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * API platform settings entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiSettings(
    /**
     * Default per-user rate limit per minute across all keys and login-state calls (0 = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    var defaultUserRateLimitPerMin: Int? = null,
    /**
     * Default per-IP rate limit per minute for anonymous callers (0 = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    var defaultAnonymousRateLimitPerMin: Int? = null,
    /**
     * Per-IP authentication failure limit per minute (0 = disabled, unset = 30)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    var authFailureLimitPerMin: Int? = null,
    /**
     * Default per-user quota requests per period (0 = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var defaultQuota: Long? = null,
    /**
     * Default per-user quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var defaultQuotaPeriodSeconds: Int? = null,
    /**
     * Length of generated access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var accessKeyLength: Int? = null,
    /**
     * Length of generated secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var secretKeyLength: Int? = null,
    /**
     * Whether balance check is enable before billing
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var balanceCheckEnable: Boolean? = null,
    /**
     * TTL of api key cache in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var cacheTtlSeconds: Int? = null,
)
