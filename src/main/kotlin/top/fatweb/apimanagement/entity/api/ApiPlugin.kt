package top.fatweb.apimanagement.entity.api

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
@TableName("t_b_api_plugin")
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
     * Unique plugin ID, e.g. "echo"
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
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

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
     * Default access mode inherited by interfaces whose access_mode is null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    @TableField("default_access_mode")
    var defaultAccessMode: ApiInterface.AccessMode? = null

    /**
     * Plugin source: BUILT_IN or UPLOADED
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("source")
    var source: String? = null

    /**
     * Plugin version name, e.g. "1.2.0"
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("version_name")
    var versionName: String? = null

    /**
     * Plugin version code (monotonic; upgrades require a strictly greater value)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("version_code")
    var versionCode: Int? = null

    /**
     * Plugin jar file hash (t_s_storage_blob.file_hash)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("file_hash")
    var fileHash: String? = null

    /**
     * Original jar file name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("jar_name")
    var jarName: String? = null

    /**
     * Signer public key fingerprint (t_b_api_plugin_trust_key.key_id)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("signer_key_id")
    var signerKeyId: String? = null

    /**
     * Embedded OpenAPI fragment JSON (from META-INF/plugin-openapi.json)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("openapi")
    var openapi: String? = null

    /**
     * Last mount error message (null when the plugin loaded successfully)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("load_error")
    var loadError: String? = null

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
        return "ApiPlugin(id=$id, pluginId=$pluginId, name=$name, description=$description, enable=$enable, defaultPrice=$defaultPrice, defaultRateLimit=$defaultRateLimit, defaultAccessMode=$defaultAccessMode, source=$source, versionName=$versionName, versionCode=$versionCode, fileHash=$fileHash, jarName=$jarName, signerKeyId=$signerKeyId, loadError=$loadError, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version)"
    }
}
