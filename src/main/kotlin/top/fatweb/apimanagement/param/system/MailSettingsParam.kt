package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.settings.MailSecurityType

/**
 * Mail settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "邮件设置请求参数")
data class MailSettingsParam(
    /**
     * Host
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "SMTP 服务器")
    @field:NotBlank(message = "Host can not be blank")
    var host: String?,

    /**
     * Port
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "端口号")
    @field:NotBlank(message = "Port can not be null")
    var port: Int?,

    /**
     * Security type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "安全类型", allowableValues = ["None", "SSL/TLS", "StartTls"], defaultValue = "None")
    val securityType: MailSecurityType = MailSecurityType.NONE,

    /**
     * Username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户名")
    @field:NotBlank(message = "Username can not be blank")
    var username: String?,

    /**
     * Password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密码")
    @field:NotBlank(message = "Password can not be blank")
    val password: String?,

    /**
     * Sender
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发送者")
    @field:NotBlank(message = "Sender can not be blank")
    var from: String?,

    /**
     * Sender name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发送者名称")
    @field:NotBlank(message = "Sender name can not be blank")
    var fromName: String?
)
