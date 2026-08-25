package top.fatweb.apimanagement.param.permission

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.CaptchaCodeParam

/**
 * Login parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see CaptchaCodeParam
 */
@ParamProcessor
@Schema(description = "登录请求参数")
data class LoginParam(
    /**
     * Account
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "账户", required = true, example = "test")
    @field:NotBlank(message = "Account can not be blank")
    var account: String?,

    /**
     * Password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密码", required = true)
    @field:NotBlank(message = "Password can not be blank")
    val password: String?,

    /**
     * Two-factor code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "双因素验证码")
    val twoFactorCode: String?
) : CaptchaCodeParam()
