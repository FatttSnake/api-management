package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update API plugin enable status parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件启用状态修改请求参数")
data class ApiPluginUpdateStatusParam(
    /**
     * API plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 插件 ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", required = true, example = "true")
    @field:NotNull(message = "Status can not be null")
    var enable: Boolean?
)
