package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Base settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "基础设置请求参数")
data class BaseSettingsParam(
    /**
     * System name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "系统名称")
    @field:NotBlank(message = "System name can not be blank")
    var systemName: String?,

    /**
     * Token expiry buffer time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Token 失效缓冲时间（毫秒）")
    @field:NotNull(message = "Token expiry buffer (ms) can not be null")
    var tokenExpiryBufferMs: Long?,

    /**
     * Token expiry check interval time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Token 失效检查周期（毫秒）")
    @field:NotNull(message = "Token expiry check interval (ms) can not be null")
    var tokenExpiryCheckIntervalMs: Long?,

    /**
     * Turnstile site key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Turnstile 站点标识")
    var turnstileSiteKey: String?,

    /**
     * Turnstile secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Turnstile 密钥")
    var turnstileSecretKey: String?,

    /**
     * Home URL
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "主页 URL")
    @field:NotBlank(message = "Home URL can not be blank")
    var homeUrl: String?
)
