package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode

/**
 * API documentation plugin value object
 *
 * Served to end users at `/user/api/docs` so the frontend can render callable API
 * documentation: plugin summary plus its enabled interfaces and (optionally) the
 * embedded OpenAPI fragment with parameter / schema details.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 文档插件返回参数")
data class ApiDocVo(
    /**
     * Plugin ID
     */
    @field:Schema(description = "插件 ID", example = "echo")
    val pluginId: String?,

    /**
     * Plugin display name
     */
    @field:Schema(description = "插件名称", example = "Echo 插件")
    val name: String?,

    /**
     * Plugin description
     */
    @field:Schema(description = "插件描述")
    val description: String?,

    /**
     * Plugin version name
     */
    @field:Schema(description = "插件版本名", example = "1.0.0")
    val versionName: String?,

    /**
     * Plugin version code
     */
    @field:Schema(description = "插件版本号", example = "1")
    val versionCode: Int?,

    /**
     * Enable status
     */
    @field:Schema(description = "启用", example = "true")
    val enable: Boolean?,

    /**
     * Enabled interfaces of the plugin
     */
    @field:Schema(description = "该插件下的接口列表")
    val interfaces: List<ApiInterfaceVo>,

    /**
     * Embedded OpenAPI fragment (paths / components.schemas), null when absent
     */
    @field:Schema(description = "内嵌 OpenAPI 片段")
    val openapi: JsonNode?
)
