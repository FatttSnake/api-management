package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Mail send parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "邮件发送请求参数")
data class MailSendParam(
    /**
     * Receiver
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "接收者", required = true, example = "user@email.com")
    @field:NotBlank(message = "Receiver cannot be blank")
    var to: String?
)
