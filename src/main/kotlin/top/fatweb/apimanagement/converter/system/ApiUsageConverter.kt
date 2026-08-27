package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiUsageVo

/**
 * Convert to ApiUsageVo object
 *
 * @return ApiUsageVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiUsage
 * @see ApiUsageVo
 */
fun ApiUsage.toVo() = ApiUsageVo(
    id = this.id,
    apiKeyId = this.apiKeyId,
    apiId = this.apiId,
    apiCode = this.apiCode,
    userId = this.userId,
    requestPath = this.requestPath,
    requestMethod = this.requestMethod,
    responseCode = this.responseCode,
    success = this.success?.let { it == 1 },
    executeTime = this.executeTime,
    requestIp = this.requestIp,
    traceId = this.traceId,
    cost = this.cost,
    billingMode = this.billingMode,
    createTime = this.createTime
)

/**
 * Convert to PageVo<ApiUsageVo> object
 *
 * @return PageVo<ApiUsageVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiUsage
 * @see PageVo
 */
fun IPage<ApiUsage>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiUsage::toVo)
)
