package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiUsageVo

/**
 * Convert to ApiUsageVo object
 *
 * @return ApiUsageVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiUsage
 * @see ApiUsageVo
 */
fun ApiUsage.toVo(resolveApi: ((String) -> ApiInterface?)? = null): ApiUsageVo {
    val api = apiCode?.let { code -> resolveApi?.invoke(code) }
    return ApiUsageVo(
        id = this.id,
        apiKeyId = this.apiKeyId,
        apiId = this.apiId,
        apiCode = this.apiCode,
        apiName = api?.name,
        apiDescription = api?.description,
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
}

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
fun IPage<ApiUsage>.toVoPage(resolveApi: ((String) -> ApiInterface?)? = null) = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map { it.toVo(resolveApi) }
)
