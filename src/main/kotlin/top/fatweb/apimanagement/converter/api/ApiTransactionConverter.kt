package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiTransaction
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiTransactionVo

/**
 * Convert to ApiTransactionVo object
 *
 * @return ApiTransactionVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiTransaction
 * @see ApiTransactionVo
 */
fun ApiTransaction.toVo() = ApiTransactionVo(
    id = this.id,
    userId = this.userId,
    apiKeyId = this.apiKeyId,
    apiUsageId = this.apiUsageId,
    orderNo = this.orderNo,
    type = this.type,
    amount = this.amount,
    balanceAfter = this.balanceAfter,
    remark = this.remark,
    createTime = this.createTime
)

/**
 * Convert to PageVo<ApiTransactionVo> object
 *
 * @return PageVo<ApiTransactionVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiTransaction
 * @see PageVo
 */
fun IPage<ApiTransaction>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiTransaction::toVo)
)
