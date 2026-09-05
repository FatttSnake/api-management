package top.fatweb.apimanagement.entity.api

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable
import java.time.LocalDateTime

/**
 * API key entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_b_api_key")
class ApiKey : Serializable {
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
     * Access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("access_key")
    var accessKey: String? = null

    /**
     * Secret key SHA-256 hash
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("secret_key_hash")
    var secretKeyHash: String? = null

    /**
     * Key name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("name")
    var name: String? = null

    /**
     * Scoped API codes snapshot
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("permissions")
    var permissions: String? = null

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

    /**
     * Expire time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("expire_time")
    var expireTime: LocalDateTime? = null

    /**
     * IP whitelist
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("ip_whitelist")
    var ipWhitelist: String? = null

    /**
     * Per-key rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("rate_limit")
    var rateLimit: Int? = null

    /**
     * Quota requests per period
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("quota")
    var quota: Long? = null

    /**
     * Quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("quota_period")
    var quotaPeriod: Int? = null

    /**
     * Last used time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("last_used_time")
    var lastUsedTime: LocalDateTime? = null

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
        return "ApiKey(id=$id, userId=$userId, accessKey=$accessKey, secretKeyHash=$secretKeyHash, name=$name, permissions=$permissions, enable=$enable, expireTime=$expireTime, ipWhitelist=$ipWhitelist, rateLimit=$rateLimit, quota=$quota, quotaPeriod=$quotaPeriod, lastUsedTime=$lastUsedTime, remark=$remark, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version)"
    }
}
