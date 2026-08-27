package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API registry entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_api")
class Api : Serializable {
    /**
     * Billing mode enum
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    enum class BillingMode(@field:EnumValue @field:JsonValue val code: String) {
        FREE("FREE"), SUCCESS_ONLY("SUCCESS_ONLY"), ALWAYS("ALWAYS")
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
     * API scoping code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("code")
    var code: String? = null

    /**
     * API name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("name")
    var name: String? = null

    /**
     * Description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("description")
    var description: String? = null

    /**
     * Request path
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("path")
    var path: String? = null

    /**
     * HTTP method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("method")
    var method: String? = null

    /**
     * API version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("api_version")
    var apiVersion: Int? = null

    /**
     * Price per call
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @TableField("price")
    var price: BigDecimal? = null

    /**
     * Billing mode
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BillingMode
     */
    @TableField("billing_mode")
    var billingMode: BillingMode? = null

    /**
     * Need API key status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("need_key")
    var needKey: Int? = null

    /**
     * Per-API rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("rate_limit")
    var rateLimit: Int? = null

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enabled")
    var enabled: Int? = null

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
        return "Api(id=$id, code=$code, name=$name, path=$path, method=$method, apiVersion=$apiVersion, price=$price, billingMode=$billingMode, needKey=$needKey, rateLimit=$rateLimit, enabled=$enabled)"
    }
}
