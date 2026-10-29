package top.fatweb.apimanagement.converter.api

import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.vo.api.ApiInterfaceVo
import java.math.BigDecimal

/**
 * Convert to ApiInterfaceVo object
 *
 * @return ApiInterfaceVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterfaceVo
 */
fun ApiInterface.toVo() = ApiInterfaceVo(
    id = this.id,
    pluginId = this.pluginId,
    code = this.code,
    name = this.name,
    description = this.description,
    path = this.path,
    method = this.method,
    apiVersion = this.apiVersion,
    price = this.price,
    billingMode = this.billingMode,
    needKey = this.needKey?.let { it == 1 },
    rateLimit = this.rateLimit,
    enable = this.enable?.let { it == 1 },
    accessMode = this.accessMode,
    createTime = this.createTime,
    updateTime = this.updateTime,
)

/**
 * Convert to an ApiInterfaceVo whose inherited values are resolved to the effective values
 * the gateway enforces at call time, so a caller-facing page needs no knowledge of plugin
 * defaults and never renders an "inherit" placeholder.
 *
 * price is never null and "0" means free; rateLimit is never null and 0 means unlimited.
 *
 * @param plugin Owning plugin, or null when it is no longer registered
 * @return Resolved ApiInterfaceVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterfaceVo
 */
fun ApiInterface.toResolvedVo(plugin: ApiPlugin?) = ApiInterfaceVo(
    id = this.id,
    pluginId = this.pluginId,
    code = this.code,
    name = this.name,
    description = this.description,
    path = this.path,
    method = this.method,
    apiVersion = this.apiVersion,
    price = this.price ?: plugin?.defaultPrice ?: BigDecimal.ZERO,
    billingMode = this.billingMode ?: ApiInterface.BillingMode.SUCCESS_ONLY,
    needKey = this.needKey?.let { it == 1 },
    rateLimit = this.rateLimit ?: plugin?.defaultRateLimit ?: 0,
    enable = this.enable?.let { it == 1 },
    accessMode = this.accessMode ?: plugin?.defaultAccessMode ?: ApiInterface.AccessMode.RESTRICTED,
    createTime = this.createTime,
    updateTime = this.updateTime,
)
