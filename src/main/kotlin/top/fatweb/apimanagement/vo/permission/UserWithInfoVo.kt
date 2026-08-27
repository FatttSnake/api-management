package top.fatweb.apimanagement.vo.permission

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.vo.permission.base.UserInfoVo
import java.time.LocalDateTime

/**
 * User with information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "用户信息返回参数")
data class UserWithInfoVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户名", example = "User")
    val username: String?,

    /**
     * Two-factor enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用双因素", example = "true")
    val twoFactor: Boolean?,

    /**
     * Verified
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "已验证", example = "true")
    val verified: Boolean?,

    /**
     * Locking
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否锁定", example = "false")
    val locking: Boolean?,

    /**
     * Expiration time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "过期时间", example = "1900-01-01T00:00:00.000Z")
    val expiration: LocalDateTime?,

    /**
     * Credentials expiration time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "认证过期时间", example = "1900-01-01T00:00:00.000Z")
    val credentialsExpiration: LocalDateTime?,

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否启用", example = "true")
    val enable: Boolean?,

    /**
     * Current login time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "当前登录时间", example = "1900-01-01T00:00:00.000Z")
    val currentLoginTime: LocalDateTime?,

    /**
     * Current login IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "当前登录 IP", example = "1.1.1.1")
    val currentLoginIp: String?,

    /**
     * Last login time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "最后登录时间", example = "1900-01-01T00:00:00.000Z")
    val lastLoginTime: LocalDateTime?,

    /**
     * Last login IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最后登录 IP", example = "1.1.1.1")
    val lastLoginIp: String?,

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "创建时间", example = "1900-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?,

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "修改时间", example = "1900-01-01T00:00:00.000Z")
    val updateTime: LocalDateTime?,

    /**
     * User information object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserInfoVo
     */
    @field:Schema(description = "用户资料")
    val userInfo: UserInfoVo?
)
