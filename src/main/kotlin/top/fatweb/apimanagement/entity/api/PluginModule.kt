package top.fatweb.apimanagement.entity.api

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.annotation.Version
import java.io.Serializable
import java.time.LocalDateTime

/**
 * Plugin dependency module entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_b_plugin_module")
class PluginModule : Serializable {
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Maven group
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("group_id")
    var groupId: String? = null

    /**
     * Maven artifact
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("artifact_id")
    var artifactId: String? = null

    /**
     * Resolved Maven version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("module_version")
    var moduleVersion: String? = null

    /**
     * Module jar SHA-256
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("sha256")
    var sha256: String? = null

    /**
     * Module jar size in bytes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("size")
    var size: Long? = null

    /**
     * Repository the module was resolved from
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("source")
    var source: String? = null

    /**
     * Install time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("install_time")
    var installTime: LocalDateTime? = null

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("create_time", fill = FieldFill.INSERT)
    var createTime: LocalDateTime? = null

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("update_time", fill = FieldFill.INSERT_UPDATE)
    var updateTime: LocalDateTime? = null

    /**
     * Optimistic lock version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("version")
    @Version
    var version: Int? = null

    /**
     * Maven coordinate, or the digest while the module is staged and has no coordinate yet
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val coordinate: String
        get() {
            val parts = listOfNotNull(groupId, artifactId, moduleVersion)
            return if (parts.size == 3) parts.joinToString(":") else "sha256:$sha256"
        }

    override fun toString(): String =
        "PluginModule(id=$id, groupId=$groupId, artifactId=$artifactId, moduleVersion=$moduleVersion, sha256=$sha256)"
}
