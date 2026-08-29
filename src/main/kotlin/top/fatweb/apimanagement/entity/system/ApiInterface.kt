package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API interface entity
 *
 * One row per registered interface (endpoint) of a plugin. Price and rate limit
 * are nullable: when null, the caller inherits the owning plugin's defaults.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_api_interface")
class ApiInterface : Serializable {
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
     * Access mode enum
     *
     * DEFAULT means normal (account/password) users may call the API without an
     * admin grant; RESTRICTED means they must hold the api:* operation code. This
     * only governs the account/JWT path — an AccessKey always needs the code in its
     * permissions regardless of mode. Null on an interface means inherit the plugin.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    enum class AccessMode(@field:EnumValue @field:JsonValue val code: String) {
        DEFAULT("DEFAULT"), RESTRICTED("RESTRICTED")
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
     * Owning plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("plugin_id")
    var pluginId: String? = null

    /**
     * API scoping code, e.g. api:avatar:v1:getRandom
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
     * Price per call (null = inherit plugin default)
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
     * Rate limit per minute (null = inherit plugin default)
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
     * Access mode (null = inherit owning plugin's default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AccessMode
     */
    @TableField("access_mode")
    var accessMode: AccessMode? = null

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
        return "ApiInterface(id=$id, pluginId=$pluginId, code=$code, name=$name, description=$description, path=$path, method=$method, apiVersion=$apiVersion, price=$price, billingMode=$billingMode, needKey=$needKey, rateLimit=$rateLimit, enabled=$enabled, accessMode=$accessMode, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version)"
    }
}
