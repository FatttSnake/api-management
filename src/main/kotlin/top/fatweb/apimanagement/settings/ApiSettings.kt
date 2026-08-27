package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * API platform settings entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiSettings(
    /**
     * Default rate limit per minute per key (0 = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var defaultRateLimitPerMin: Int? = null,

    /**
     * Default quota requests per period per key (0 = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var defaultQuota: Long? = null,

    /**
     * Default quota period in seconds
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
     * Whether balance check is enabled before billing
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var balanceCheckEnabled: Boolean? = null,

    /**
     * TTL of api key cache in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var cacheTtlSeconds: Int? = null
)
