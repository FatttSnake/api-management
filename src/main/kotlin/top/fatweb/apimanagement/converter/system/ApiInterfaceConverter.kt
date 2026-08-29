package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.ApiInterface
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiInterfaceVo

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
    enabled = this.enabled?.let { it == 1 },
    accessMode = this.accessMode,
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to PageVo<ApiInterfaceVo> object
 *
 * @return PageVo<ApiInterfaceVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiInterface
 * @see PageVo
 */
fun IPage<ApiInterface>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiInterface::toVo)
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
    enabled = this@toEntity.enabled?.let { if (it) 1 else 0 }
    accessMode = this@toEntity.accessMode
}
