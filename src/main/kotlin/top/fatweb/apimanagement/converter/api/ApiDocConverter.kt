package top.fatweb.apimanagement.converter.api

import tools.jackson.databind.JsonNode
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.vo.api.ApiDocVo
import top.fatweb.apimanagement.vo.api.ApiInterfaceVo

/**
 * Convert to ApiDocVo object
 *
 * @param interfaces Enabled interfaces of the plugin
 * @param openapi Parsed embedded OpenAPI fragment, or null
 * @return ApiDocVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPlugin
 * @see ApiDocVo
 */
fun ApiPlugin.toDocVo(interfaces: List<ApiInterfaceVo>, openapi: JsonNode?) = ApiDocVo(
    pluginId = this.pluginId,
    name = this.name,
    description = this.description,
    versionName = this.versionName,
    versionCode = this.versionCode,
    enable = this.enable?.let { it == 1 },
    interfaces = interfaces,
    openapi = openapi
)
