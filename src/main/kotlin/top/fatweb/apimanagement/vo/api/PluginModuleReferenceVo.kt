package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Plugin that references a dependency module.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "引用 API 插件依赖模块的插件")
data class PluginModuleReferenceVo(
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件 ID")
    val pluginId: String?,
    /**
     * Plugin name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件名称")
    val name: String?,
    /**
     * Whether the plugin is enabled
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件是否启用")
    val enable: Boolean?,
)
