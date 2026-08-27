package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.ApiPlugin
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiPluginVo

/**
 * Convert to ApiPluginVo object
 *
 * @return ApiPluginVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPlugin
 * @see ApiPluginVo
 */
fun ApiPlugin.toVo() = ApiPluginVo(
    id = this.id,
    pluginId = this.pluginId,
    name = this.name,
    description = this.description,
    enabled = this.enabled?.let { it == 1 },
    defaultPrice = this.defaultPrice,
    defaultRateLimit = this.defaultRateLimit,
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to PageVo<ApiPluginVo> object
 *
 * @return PageVo<ApiPluginVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiPlugin
 * @see PageVo
 */
fun IPage<ApiPlugin>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiPlugin::toVo)
)

/**
 * Convert to ApiPlugin object
 *
 * @return ApiPlugin object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPluginUpdateParam
 * @see ApiPlugin
 */
fun ApiPluginUpdateParam.toEntity() = ApiPlugin().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    description = this@toEntity.description
    enabled = this@toEntity.enabled?.let { if (it) 1 else 0 }
    defaultPrice = this@toEntity.defaultPrice
    defaultRateLimit = this@toEntity.defaultRateLimit
}
