package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import tools.jackson.databind.JsonNode
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.vo.PageVo
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

/**
 * Convert to PageVo<ApiDocVo> object
 *
 * @return PageVo<ApiDocVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JsonNode
 * @see PageVo
 */
fun IPage<ApiPlugin>.toDocVoPage(resolveInterfaces: ((ApiPlugin) -> List<ApiInterfaceVo>)) = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map { plugin ->
        plugin.toDocVo(interfaces = resolveInterfaces.invoke(plugin), openapi = null)
    }
)
