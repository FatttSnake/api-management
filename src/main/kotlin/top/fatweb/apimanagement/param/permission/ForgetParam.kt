package top.fatweb.apimanagement.param.permission

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import top.fatweb.apimanagement.param.CaptchaCodeParam

/**
 * Forget password parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see CaptchaCodeParam
 */
@Schema(description = "忘记密码请求参数")
data class ForgetParam(
    /**
     * Email
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "邮箱", required = true, example = "user@email.com")
    @field:NotBlank(message = "Email can not be blank")
    @field:Pattern(regexp = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", message = "Illegal email address")
    var email: String?
) : CaptchaCodeParam()
