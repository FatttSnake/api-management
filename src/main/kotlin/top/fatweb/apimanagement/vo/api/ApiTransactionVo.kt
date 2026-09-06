package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.api.ApiTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API transaction value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 流水返回参数")
data class ApiTransactionVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Owner user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: Long?,

    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val apiKeyId: Long?,

    /**
     * API usage ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val apiUsageId: Long?,

    /**
     * Top-up order number
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "充值订单号", example = "ORDER-20260825-001")
    val orderNo: String?,

    /**
     * Transaction type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTransaction.Type
     */
    @field:Schema(description = "流水类型", allowableValues = ["TOPUP", "DEDUCT", "REFUND", "ADJUST"])
    val type: ApiTransaction.Type?,

    /**
     * Signed amount
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "金额（正=充值 负=扣费）", example = "-0.0100")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val amount: BigDecimal?,

    /**
     * Balance after transaction
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "交易后余额", example = "99.9900")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val balanceAfter: BigDecimal?,

    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "备注")
    val remark: String?,

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "交易时间", example = "2026-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?
)
