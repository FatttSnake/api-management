package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Two-factor settings value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "双因素设置返回参数")
data class TwoFactorSettingsVo(
    /**
     * Issuer
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "发布者")
    val issuer: String?,

    /**
     * Length of secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密钥长度")
    val secretKeyLength: Int?
)
