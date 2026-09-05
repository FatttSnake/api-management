package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.exception.ApiUsageIdNotGeneratedException
import top.fatweb.apimanagement.mapper.api.ApiUsageMapper
import top.fatweb.apimanagement.param.system.apiUsage.ApiUsageGetParam
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.service.api.IApiUsageService
import top.fatweb.apimanagement.util.getLoginUserIdOrThrow
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiUsageVo

/**
 * API usage service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ApiUsageMapper
 * @see ApiUsage
 * @see IApiUsageService
 */
@Service
@DS("master")
class ApiUsageServiceImpl(
    private val apiPluginService: IApiPluginService,
    private val apiKeyService: IApiKeyService
) : ServiceImpl<ApiUsageMapper, ApiUsage>(), IApiUsageService {
    override fun getPage(managed: Boolean, apiUsageGetParam: ApiUsageGetParam?): PageVo<ApiUsageVo> {
        val page = Page<ApiUsage>(apiUsageGetParam?.currentPage ?: 1, apiUsageGetParam?.pageSize ?: 20)
        setPageSort(apiUsageGetParam, page, OrderItem.desc("create_time"))

        val loginUserId = getLoginUserIdOrThrow()
        val targetUserId = if (managed) apiUsageGetParam?.userId ?: loginUserId else loginUserId
        val wrapper = KtQueryWrapper(ApiUsage()).apply {
            eq(ApiUsage::userId, targetUserId)
            apiUsageGetParam?.apiKeyId?.let { eq(ApiUsage::apiKeyId, it) }
            apiUsageGetParam?.apiCode?.let { like(ApiUsage::apiCode, it) }
            apiUsageGetParam?.success?.let { eq(ApiUsage::success, if (it) 1 else 0) }
            apiUsageGetParam?.startTime?.let { ge(ApiUsage::createTime, it) }
            apiUsageGetParam?.endTime?.let { le(ApiUsage::createTime, it) }
        }

        return page(page, wrapper).toVoPage { apiPluginService.getByCode(it) }
    }

    override fun saveUsage(apiUsage: ApiUsage): Long {
        saveOrThrowException { save(apiUsage) }
        // A persisted usage row means the key actually executed an interface call (rejected
        // and rate-limited requests never reach here) — refresh its last used time. Best
        // effort and throttled; already running off the request thread.
        apiUsage.apiKeyId?.let { runCatching { apiKeyService.touchLastUsedTime(it) } }
        return apiUsage.id ?: throw ApiUsageIdNotGeneratedException()
    }
}
