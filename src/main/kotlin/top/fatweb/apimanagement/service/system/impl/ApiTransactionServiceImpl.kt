package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.converter.system.toVo
import top.fatweb.apimanagement.converter.system.toVoPage
import top.fatweb.apimanagement.entity.system.ApiTransaction
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.mapper.system.ApiTransactionMapper
import top.fatweb.apimanagement.param.system.apiAccount.ApiTransactionGetParam
import top.fatweb.apimanagement.service.system.IApiTransactionService
import top.fatweb.apimanagement.util.getLoginUserIdOrThrow
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiTransactionVo
import java.math.BigDecimal

/**
 * API transaction service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ApiTransactionMapper
 * @see ApiTransaction
 * @see IApiTransactionService
 */
@Service
@DS("master")
class ApiTransactionServiceImpl : ServiceImpl<ApiTransactionMapper, ApiTransaction>(), IApiTransactionService {
    override fun getPage(
        managed: Boolean,
        apiTransactionGetParam: ApiTransactionGetParam?
    ): PageVo<ApiTransactionVo> {
        val page = Page<ApiTransaction>(
            apiTransactionGetParam?.currentPage ?: 1, apiTransactionGetParam?.pageSize ?: 20
        )
        setPageSort(apiTransactionGetParam, page, OrderItem.desc("create_time"))

        val targetUserId = if (managed) apiTransactionGetParam?.userId ?: getLoginUserIdOrThrow()
        else getLoginUserIdOrThrow()
        val wrapper = KtQueryWrapper(ApiTransaction()).apply {
            eq(ApiTransaction::userId, targetUserId)
            apiTransactionGetParam?.type?.let { eq(ApiTransaction::type, it) }
            apiTransactionGetParam?.startTime?.let { ge(ApiTransaction::createTime, it) }
            apiTransactionGetParam?.endTime?.let { le(ApiTransaction::createTime, it) }
        }

        return this.page(page, wrapper).toVoPage()
    }

    override fun getByOrderNo(orderNo: String): ApiTransaction? =
        this.getOne(KtQueryWrapper(ApiTransaction()).eq(ApiTransaction::orderNo, orderNo))

    @Transactional
    override fun saveDeduct(apiUsage: ApiUsage, balanceAfter: BigDecimal): Boolean {
        val usageId = apiUsage.id ?: return false
        if (this.count(KtQueryWrapper(ApiTransaction()).eq(ApiTransaction::apiUsageId, usageId)) > 0) {
            return true
        }

        val transaction = ApiTransaction().apply {
            userId = apiUsage.userId
            apiKeyId = apiUsage.apiKeyId
            apiUsageId = usageId
            type = ApiTransaction.Type.DEDUCT
            amount = apiUsage.cost?.negate()
            this.balanceAfter = balanceAfter
            remark = "API usage ${apiUsage.apiCode}"
        }

        return try {
            this.save(transaction)
        } catch (_: DuplicateKeyException) {
            true
        }
    }
}
