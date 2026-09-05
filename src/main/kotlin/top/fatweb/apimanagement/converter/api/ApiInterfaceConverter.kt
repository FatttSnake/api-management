package top.fatweb.apimanagement.converter.api

import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateStatusParam
import top.fatweb.apimanagement.vo.api.ApiInterfaceVo

/**
 * Convert to ApiInterfaceVo object
 *
 * @return ApiInterfaceVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterface
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
    updateTime = this.updateTime
)

/**
 * Convert to ApiInterface object
 *
 * @return ApiInterface object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterfaceUpdateParam
 * @see ApiInterface
 */
fun ApiInterfaceUpdateParam.toEntity() = ApiInterface().apply {
    id = this@toEntity.id
    price = this@toEntity.price
    billingMode = this@toEntity.billingMode
    needKey = this@toEntity.needKey?.let { if (it) 1 else 0 }
    rateLimit = this@toEntity.rateLimit
    enable = this@toEntity.enable?.let { if (it) 1 else 0 }
    accessMode = this@toEntity.accessMode
}

/**
 * Convert to ApiInterface object
 *
 * @return ApiInterface object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterfaceUpdateStatusParam
 * @see ApiInterface
 */
fun ApiInterfaceUpdateStatusParam.toEntity() = ApiInterface().apply {
    id = this@toEntity.id
    enable = if (this@toEntity.enable == true) 1 else 0
}
