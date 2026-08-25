package top.fatweb.apimanagement.entity.permission

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable
import java.time.LocalDateTime

/**
 * User entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_user")
class User() : Serializable {
    constructor(id: Long?, username: String, password: String, enable: Boolean = true) : this() {
        this.id = id
        this.username = username
        this.password = password
        this.enable = if (enable) 1 else 0
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
     * Username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("username")
    var username: String? = null

    /**
     * Password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("password")
    var password: String? = null

    /**
     * Two-factor
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("two_factor")
    var twoFactor: String? = null

    /**
     * Verify email
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("verify")
    var verify: String? = null

    /**
     * Forget password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("forget")
    var forget: String? = null

    /**
     * Locking
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("locking")
    var locking: Int? = null

    /**
     * Expiration time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("expiration")
    var expiration: LocalDateTime? = null

    /**
     * Credentials expiration time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("credentials_expiration")
    var credentialsExpiration: LocalDateTime? = null

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

    /**
     * Current login time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("current_login_time")
    var currentLoginTime: LocalDateTime? = null

    /**
     * Current login IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("current_login_ip")
    var currentLoginIp: String? = null

    /**
     * Last login time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("last_login_time")
    var lastLoginTime: LocalDateTime? = null

    /**
     * Last login IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("last_login_ip")
    var lastLoginIp: String? = null

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("create_time")
    var createTime: LocalDateTime? = null

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @TableField("update_time")
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
     * User information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserInfo
     */
    @TableField(exist = false)
    var userInfo: UserInfo? = null

    /**
     * Role list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Role
     */
    @TableField(exist = false)
    var roles: List<Role>? = null

    /**
     * Group list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Group
     */
    @TableField(exist = false)
    var groups: List<Group>? = null

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

    override fun toString(): String {
        return "User(id=$id, username=$username, password=$password, twoFactor=$twoFactor, verify=$verify, forget=$forget, locking=$locking, expiration=$expiration, credentialsExpiration=$credentialsExpiration, enable=$enable, currentLoginTime=$currentLoginTime, currentLoginIp=$currentLoginIp, lastLoginTime=$lastLoginTime, lastLoginIp=$lastLoginIp, createTime=$createTime, updateTime=$updateTime, deleted=$deleted, version=$version, userInfo=$userInfo, roles=$roles, groups=$groups, modules=$modules, menus=$menus, scopes=$scopes, operations=$operations)"
    }
}
