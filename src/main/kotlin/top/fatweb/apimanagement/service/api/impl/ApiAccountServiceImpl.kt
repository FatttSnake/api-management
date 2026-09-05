package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.api.ApiAccount
import top.fatweb.apimanagement.entity.api.ApiTransaction
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.exception.ApiAccountNotFoundException
import top.fatweb.apimanagement.mapper.api.ApiAccountMapper
import top.fatweb.apimanagement.param.system.apiAccount.ApiTopUpParam
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiTransactionService
import top.fatweb.apimanagement.util.getLoginUserIdOrThrow
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.vo.api.ApiAccountVo
import top.fatweb.apimanagement.vo.api.ApiTransactionVo
import java.math.BigDecimal

/**
 * API account service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiTransactionService
 * @see ServiceImpl
 * @see ApiAccountMapper
 * @see ApiAccount
 * @see IApiAccountService
 */
@Service
@DS("master")
class ApiAccountServiceImpl(
    private val apiTransactionService: IApiTransactionService
) : ServiceImpl<ApiAccountMapper, ApiAccount>(), IApiAccountService {
    override fun getAccount(managed: Boolean, userId: Long?): ApiAccountVo =
        getOrCreate(resolveTarget(managed, userId)).toVo()

    override fun getBalance(userId: Long): BigDecimal = getOrCreate(userId).balance ?: BigDecimal.ZERO

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_TOPUP)
    override fun topUp(managed: Boolean, apiTopUpParam: ApiTopUpParam): ApiTransactionVo {
        val targetUserId = resolveTarget(managed, apiTopUpParam.userId)

        apiTopUpParam.orderNo?.let { orderNo ->
            apiTransactionService.getByOrderNo(orderNo)?.let { return it.toVo() }
        }

        val account = getOrCreate(targetUserId)
        val amount = apiTopUpParam.amount ?: throw ApiAccountNotFoundException()
        updateOrThrowException { baseMapper.topUp(account.id!!, amount) > 0 }

        val transaction = ApiTransaction().apply {
            this.userId = targetUserId
            this.orderNo = apiTopUpParam.orderNo
            this.type = ApiTransaction.Type.TOPUP
            this.amount = amount
            this.balanceAfter = getBalance(targetUserId)
            this.remark = apiTopUpParam.remark
        }
        saveOrThrowException { apiTransactionService.save(transaction) }
        return transaction.toVo()
    }

    override fun deduct(userId: Long, cost: BigDecimal): Boolean {
        if (cost.signum() <= 0) {
            return true
        }
        val account = getOrCreate(userId)
        return baseMapper.deduct(account.id!!, cost) > 0
    }

    override fun checkBalance(userId: Long, cost: BigDecimal): Boolean {
        return cost.signum() <= 0 || getBalance(userId) >= cost
    }

    private fun getOrCreate(userId: Long): ApiAccount {
        getOne(KtQueryWrapper(ApiAccount()).eq(ApiAccount::userId, userId))?.let { return it }

        val account = ApiAccount().apply {
            this.userId = userId
            this.balance = BigDecimal.ZERO
            this.enable = 1
        }
        saveOrThrowException { save(account) }
        return account
    }

    private fun resolveTarget(managed: Boolean, userId: Long?): Long =
        if (managed) userId ?: getLoginUserIdOrThrow() else getLoginUserIdOrThrow()
}
