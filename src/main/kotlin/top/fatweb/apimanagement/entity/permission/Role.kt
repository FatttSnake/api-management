package top.fatweb.apimanagement.entity.permission

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable
import java.time.LocalDateTime

/**
 * Role entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_role")
class Role : Serializable {
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("name")
    var name: String? = null

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

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

    /**
     * Module list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Module
     */
    @TableField(exist = false)
    var modules: List<Module>? = null

    /**
     * Menu list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Menu
     */
    @TableField(exist = false)
    var menus: List<Menu>? = null

    /**
     * Scope list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Scope
     */
    @TableField(exist = false)
    var scopes: List<Scope>? = null

    /**
     * Operation list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Operation
     */
    @TableField(exist = false)
    var operations: List<Operation>? = null

    /**
     * Power list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Power
     */
    @TableField(exist = false)
    var powers: List<Power>? = null

    override fun toString(): String {
        return "Role(id=$id, name=$name, enable=$enable, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version, modules=$modules, menus=$menus, scopes=$scopes, operations=$operations, powers=$powers)"
    }
}
