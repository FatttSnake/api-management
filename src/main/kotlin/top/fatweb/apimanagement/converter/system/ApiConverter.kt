package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.Api
import top.fatweb.apimanagement.param.system.api.ApiUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiVo

/**
 * Convert to ApiVo object
 *
 * @return ApiVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Api
 * @see ApiVo
 */
fun Api.toVo() = ApiVo(
    id = this.id,
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
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to PageVo<ApiVo> object
 *
 * @return PageVo<ApiVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see Api
 * @see PageVo
 */
fun IPage<Api>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(Api::toVo)
)

/**
 * Convert to Api object
 *
 * @return Api object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiUpdateParam
 * @see Api
 */
fun ApiUpdateParam.toEntity() = Api().apply {
    id = this@toEntity.id
    price = this@toEntity.price
    billingMode = this@toEntity.billingMode
    needKey = this@toEntity.needKey?.let { if (it) 1 else 0 }
    rateLimit = this@toEntity.rateLimit
    enabled = this@toEntity.enabled?.let { if (it) 1 else 0 }
}
