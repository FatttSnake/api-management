package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Two-factor settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "双因素设置请求参数")
data class TwoFactorSettingsParam(
    /**
     * Issuer
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发布者")
    @field:NotBlank(message = "Issuer can not be blank")
    var issuer: String?,

    /**
     * Length of secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密钥长度")
    @field:NotNull(message = "Length of secret key can not be null")
    @field:Min(value = 3, message = "The length of the key must be greater than or equal to 3")
    val secretKeyLength: Int?
)
