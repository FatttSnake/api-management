package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Add plugin trust key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "添加 API 插件信任密钥请求参数")
data class ApiPluginTrustKeyAddParam(
    /**
     * Public key in SPKI PEM
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "公钥 (SPKI PEM)", required = true)
    @field:NotBlank(message = "Public key can not be blank")
    var publicKey: String,

    /**
     * Key alias
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "密钥别名")
    var alias: String?
)
