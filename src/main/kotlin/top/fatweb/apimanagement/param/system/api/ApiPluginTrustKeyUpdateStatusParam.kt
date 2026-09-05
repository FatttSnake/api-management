package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update plugin trust key status parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "修改 API 插件信任密钥状态请求参数")
data class ApiPluginTrustKeyUpdateStatusParam(
    /**
     * Public key fingerprint
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "公钥指纹", required = true)
    @field:NotBlank(message = "Key ID can not be blank")
    var keyId: String,

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", required = true)
    @field:NotNull(message = "Enable can not be null")
    var enable: Boolean
)
