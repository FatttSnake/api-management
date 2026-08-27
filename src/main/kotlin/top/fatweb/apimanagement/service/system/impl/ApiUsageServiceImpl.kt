package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.system.toVoPage
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.exception.ApiUsageIdNotGeneratedException
import top.fatweb.apimanagement.mapper.system.ApiUsageMapper
import top.fatweb.apimanagement.param.system.apiUsage.ApiUsageGetParam
import top.fatweb.apimanagement.service.system.IApiUsageService
import top.fatweb.apimanagement.util.getLoginUserIdOrThrow
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiUsageVo

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
class ApiUsageServiceImpl : ServiceImpl<ApiUsageMapper, ApiUsage>(), IApiUsageService {
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

        return page(page, wrapper).toVoPage()
    }

    override fun saveUsage(apiUsage: ApiUsage): Long {
        saveOrThrowException { save(apiUsage) }
        return apiUsage.id ?: throw ApiUsageIdNotGeneratedException()
    }
}
