package top.fatweb.apimanagement.http.entity.turnstile

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Turnstile verify captcha code response
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class SiteverifyResponse(
    /**
     * Is success
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("success")
    val success: Boolean,

    /**
     * Challenge time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("challenge_ts")
    val challengeTs: LocalDateTime?,

    /**
     * Hostname
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("hostname")
    val hostname: String?,

    /**
     * Error codes list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("error-codes")
    val errorCodes: List<String>?,

    /**
     * Action
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("action")
    val action: String?,

    /**
     * Cdata
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("cdata")
    val cdata: String?,

    /**
     * Metadata
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonProperty("metadata")
    val metadata: Map<String, String>?
)
