package top.fatweb.apimanagement.entity.api

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API transaction entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_b_api_transaction")
class ApiTransaction : Serializable {
    /**
     * Transaction type enum
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    enum class Type(@field:EnumValue @field:JsonValue val code: String) {
        TOPUP("TOPUP"), DEDUCT("DEDUCT"), REFUND("REFUND"), ADJUST("ADJUST")
    }

    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Owner user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("user_id")
    var userId: Long? = null

    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("api_key_id")
    var apiKeyId: Long? = null

    /**
     * API usage ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("api_usage_id")
    var apiUsageId: Long? = null

    /**
     * Top-up order number
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("order_no")
    var orderNo: String? = null

    /**
     * Transaction type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Type
     */
    @TableField("type")
    var type: Type? = null

    /**
     * Signed amount
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @TableField("amount")
    var amount: BigDecimal? = null

    /**
     * Balance after transaction
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @TableField("balance_after")
    var balanceAfter: BigDecimal? = null

    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("remark")
    var remark: String? = null

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("create_time", fill = FieldFill.INSERT)
    var createTime: LocalDateTime? = null

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("update_time", fill = FieldFill.INSERT_UPDATE)
    var updateTime: LocalDateTime? = null

    /**
     * Deleted
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("deleted")
    @TableLogic
    var deleted: Long? = null

    /**
     * Version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("version")
    @Version
    var version: Int? = null

    override fun toString(): String {
        return "ApiTransaction(id=$id, userId=$userId, apiKeyId=$apiKeyId, apiUsageId=$apiUsageId, orderNo=$orderNo, type=$type, amount=$amount, balanceAfter=$balanceAfter, remark=$remark, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version)"
    }
}
