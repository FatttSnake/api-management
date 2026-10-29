package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo

/**
 * Convert to ApiPluginVo object
 *
 * @return ApiPluginVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPluginVo
 */
fun ApiPlugin.toVo() = ApiPluginVo(
    id = this.id,
    pluginId = this.pluginId,
    name = this.name,
    description = this.description,
    enable = this.enable?.let { it == 1 },
    defaultPrice = this.defaultPrice,
    defaultRateLimit = this.defaultRateLimit,
    defaultAccessMode = this.defaultAccessMode,
    source = this.source,
    versionName = this.versionName,
    versionCode = this.versionCode,
    jarName = this.jarName,
    signerKeyId = this.signerKeyId,
    loadError = this.loadError,
    loadStatus = this.loadStatus,
    loadErrorCode = this.loadErrorCode,
    gatewayApiVersion = this.gatewayApiVersion,
    requiresGateway = this.requiresGateway,
    requiresGatewayApi = this.requiresGatewayApi,
    createTime = this.createTime,
    updateTime = this.updateTime,
)

/**
 * Convert to PageVo<ApiPluginVo> object
 *
 * @return PageVo<ApiPluginVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageVo
 */
fun IPage<ApiPlugin>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiPlugin::toVo),
)
