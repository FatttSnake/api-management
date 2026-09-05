package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * API group value object
 *
 * Groups API interfaces by owning plugin so the frontend can render a
 * "plugin -> interface list" tree without resolving plugin names client-side.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterfaceVo
 */
@Schema(description = "API 分组返回参数")
data class ApiGroupVo(
    /**
     * Plugin ID
     */
    @Schema(description = "所属插件 ID", example = "echo")
    val pluginId: String?,

    /**
     * Plugin display name
     */
    @Schema(description = "插件名称", example = "Echo 插件")
    val pluginName: String?,

    /**
     * Plugin description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "插件描述")
    val pluginDescription: String?,

    /**
     * Interfaces belonging to the plugin
     */
    @Schema(description = "该插件下的接口列表")
    val interfaces: List<ApiInterfaceVo>
)
