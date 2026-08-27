package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.ApiTransaction
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.param.system.apiAccount.ApiTransactionGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiTransactionVo
import java.math.BigDecimal

/**
 * API transaction service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiTransaction
 */
interface IApiTransactionService : IService<ApiTransaction> {
    /**
     * Get API transaction in page
     *
     * @param managed Whether the caller is authorized to query other users' transactions
     * @param apiTransactionGetParam Get API transaction parameters
     * @return PageVo<ApiTransactionVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTransactionGetParam
     * @see PageVo
     * @see ApiTransactionVo
     */
    fun getPage(managed: Boolean, apiTransactionGetParam: ApiTransactionGetParam?): PageVo<ApiTransactionVo>

    /**
     * Get API transaction by order number
     *
     * @param orderNo Order number
     * @return ApiTransaction object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTransaction
     */
    fun getByOrderNo(orderNo: String): ApiTransaction?

    /**
     * Save deduction transaction idempotently
     *
     * @param apiUsage API usage record
     * @param balanceAfter Balance after deduction
     * @return true if saved or already exists
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUsage
     */
    fun saveDeduct(apiUsage: ApiUsage, balanceAfter: BigDecimal): Boolean
}
