package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * System setting entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class SystemSettings(
    /**
     * Base setting
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var base: BaseSettings = BaseSettings(),

    /**
     * Mail setting
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var mail: MailSettings = MailSettings(),

    /**
     * Two-factor setting
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var twoFactor: TwoFactorSettings = TwoFactorSettings(),

    /**
     * API platform setting
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var api: ApiSettings = ApiSettings()
)
