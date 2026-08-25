package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.settings.MailSecurityType

/**
 * Mail settings value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "邮件设置返回参数")
data class MailSettingsVo(
    /**
     * Host
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "SMTP 服务器")
    val host: String?,

    /**
     * Port
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "端口")
    val port: Int?,

    /**
     * Security type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "安全类型")
    val securityType: MailSecurityType?,

    /**
     * Username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户名")
    val username: String?,

    /**
     * Password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密码")
    val password: String?,

    /**
     * Sender
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发送者")
    val from: String?,

    /**
     * Sender name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发送者名称")
    val fromName: String?
)
