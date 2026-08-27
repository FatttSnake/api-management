package top.fatweb.apimanagement.param.system.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update status of API key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API Key 状态修改请求参数")
data class ApiKeyUpdateStatusParam(
    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", required = true, example = "true")
    @field:NotNull(message = "Status can not be null")
    var status: Boolean?
)
