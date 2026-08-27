package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API plugin entity
 *
 * One row per plugin. Holds the plugin's registration metadata plus plugin-level
 * defaults (default price / default rate limit) that interfaces inherit when they
 * are not configured themselves.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_api_plugin")
class ApiPlugin : Serializable {
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Unique plugin ID, e.g. "avatar"
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("plugin_id")
    var pluginId: String? = null

    /**
     * Plugin display name
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
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enabled")
    var enabled: Int? = null

    /**
     * Default price per call (null = free)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @TableField("default_price")
    var defaultPrice: BigDecimal? = null

    /**
     * Default rate limit per minute (null = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("default_rate_limit")
    var defaultRateLimit: Int? = null

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
        return "ApiPlugin(id=$id, pluginId=$pluginId, name=$name, description=$description, enabled=$enabled, defaultPrice=$defaultPrice, defaultRateLimit=$defaultRateLimit, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version)"
    }
}
