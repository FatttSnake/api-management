package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Available API group value object
 *
 * Feeds the key-authorization picker, which is a "plugin -> interface" option tree: it needs
 * a plugin's display name and, per interface, only the code it grants and the name it shows.
 * Deliberately not [ApiGroupVo] - that one carries the full interface record for the
 * administrator's interface table, and sending it here would ship thirteen fields nobody reads.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKeyAvailableInterfaceVo
 */
@Schema(description = "可授权 API 分组返回参数")
data class ApiKeyAvailableGroupVo(
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "所属插件 ID", example = "echo")
    val pluginId: String?,

    /**
     * Plugin display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "插件名称", example = "Echo 插件")
    val pluginName: String?,

    /**
     * Interfaces of the plugin that may be granted
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyAvailableInterfaceVo
     */
    @Schema(description = "该插件下可授权的接口列表")
    val interfaces: List<ApiKeyAvailableInterfaceVo>
)
