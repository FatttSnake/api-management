package top.fatweb.apimanagement.entity.api

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable
import java.time.LocalDateTime

/**
 * Plugin trust key entity
 *
 * One row per trusted plugin-signer public key. A plugin jar is only installed /
 * mounted when its embedded public key fingerprint ([keyId]) exists here and is
 * enabled, giving administrators control over which developers may publish.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_b_api_plugin_trust_key")
class ApiPluginTrustKey : Serializable {
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Public key fingerprint (SHA-256 of SPKI bytes, hex)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("key_id")
    var keyId: String? = null

    /**
     * Key alias / display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("alias")
    var alias: String? = null

    /**
     * Public key in SPKI PEM
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("public_key")
    var publicKey: String? = null

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

    /**
     * User ID who created the key entry
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("created_by")
    var createdBy: Long? = null

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
}
