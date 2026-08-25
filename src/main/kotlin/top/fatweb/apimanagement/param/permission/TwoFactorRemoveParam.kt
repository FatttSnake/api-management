package top.fatweb.apimanagement.param.permission

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * Remove two-factor parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "移除双因素请求参数")
data class TwoFactorRemoveParam(
    /**
     * Code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "验证码")
    @field:NotBlank(message = "Code can not be blank")
    val code: String?
)
