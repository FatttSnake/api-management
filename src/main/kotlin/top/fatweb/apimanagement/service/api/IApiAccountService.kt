package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiAccount
import top.fatweb.apimanagement.param.system.apiAccount.ApiTopUpParam
import top.fatweb.apimanagement.vo.api.ApiAccountVo
import top.fatweb.apimanagement.vo.api.ApiTransactionVo
import java.math.BigDecimal

/**
 * API account service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiAccount
 */
interface IApiAccountService : IService<ApiAccount> {
    /**
     * Get account value object
     *
     * @param managed Whether the caller is authorized to manage other users' accounts
     * @param userId User ID (admin only, honored when managed)
     * @return ApiAccountVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiAccountVo
     */
    fun getAccount(managed: Boolean, userId: Long?): ApiAccountVo

    /**
     * Get balance of user
     *
     * @param userId User ID
     * @return Balance
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    fun getBalance(userId: Long): BigDecimal

    /**
     * Top up balance idempotently by order number
     *
     * @param managed Whether the caller is authorized to manage other users' accounts
     * @param apiTopUpParam Top-up parameters
     * @return ApiTransactionVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTopUpParam
     * @see ApiTransactionVo
     */
    fun topUp(managed: Boolean, apiTopUpParam: ApiTopUpParam): ApiTransactionVo

    /**
     * Deduct balance atomically
     *
     * @param userId User ID
     * @param cost Cost to deduct
     * @return true if deducted; false if insufficient balance
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    fun deduct(userId: Long, cost: BigDecimal): Boolean

    /**
     * Check whether balance is sufficient
     *
     * @param userId User ID
     * @param cost Cost
     * @return true if sufficient
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    fun checkBalance(userId: Long, cost: BigDecimal): Boolean
}
