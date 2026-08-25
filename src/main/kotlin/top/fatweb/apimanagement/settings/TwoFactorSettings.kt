package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Two-factor settings entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class TwoFactorSettings(
    /**
     * Issuer
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var issuer: String? = null,

    /**
     * Length of secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var secretKeyLength: Int? = null
)
