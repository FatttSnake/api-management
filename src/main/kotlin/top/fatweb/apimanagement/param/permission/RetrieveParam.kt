package top.fatweb.apimanagement.param.permission

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import top.fatweb.apimanagement.param.CaptchaCodeParam

/**
 * Retrieve password parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see CaptchaCodeParam
 */
@Schema(description = "找回密码请求参数")
data class RetrieveParam(
    /**
     * Code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "验证码", required = true)
    @field:NotBlank(message = "Code can not be blank")
    val code: String?,

    /**
     * New password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "新密码")
    @field:NotBlank(message = "New password can not be blank")
    @field:Pattern(regexp = "^[a-fA-F0-9]{128}$", message = "New password must be encrypted by SHA512")
    val password: String?
) : CaptchaCodeParam()
