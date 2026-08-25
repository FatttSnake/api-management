package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Base settings entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class BaseSettings(
    /**
     * System name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var systemName: String? = null,

    /**
     * Token expiry buffer time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var tokenExpiryBufferMs: Long? = null,

    /**
     * Token expiry check interval time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var tokenExpiryCheckIntervalMs: Long? = null,

    /**
     * Turnstile site key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var turnstileSiteKey: String? = null,

    /**
     * Turnstile secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var turnstileSecretKey: String? = null,

    /**
     * Home URL
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var homeUrl: String? = null
)
