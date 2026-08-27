package top.fatweb.apimanagement.vo.permission

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

/**
 * Login value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "登录返回参数")
data class LoginVo(
    /**
     * Refresh Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "Refresh Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJkYTllYjFkYmVmZDQ0OWRkOThlOGNjNzZlNzZkMDgyNSIsInN1YiI6IjE3MDk5ODYwNTg2Nzk5NzU5MzgiLCJpc3MiOiJGYXRXZWIiLCJpYXQiOjE2OTY1MjgxMTcsImV4cCI6MTY5NjUzNTMxN30.U2ZsyrGk7NbsP-DJfdz9xgWSfect5r2iKQnlEsscAA8"
    )
    val refreshToken: String,

    /**
     * Access Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "Access Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJkYTllYjFkYmVmZDQ0OWRkOThlOGNjNzZlNzZkMDgyNSIsInN1YiI6IjE3MDk5ODYwNTg2Nzk5NzU5MzgiLCJpc3MiOiJGYXRXZWIiLCJpYXQiOjE2OTY1MjgxMTcsImV4cCI6MTY5NjUzNTMxN30.U2ZsyrGk7NbsP-DJfdz9xgWSfect5r2iKQnlEsscAA8"
    )
    val accessToken: String,

    /**
     * User ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "User ID",
        example = "1709986058679975938"
    )
    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: Long?,

    /**
     * Last login time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(
        description = "上次登录时间",
        example = "1900-01-01 00:00:00"
    )
    val lastLoginTime: LocalDateTime?,

    /**
     * Last login IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "上次登录 IP",
        example = "10.0.0.1"
    )
    val lastLoginIp: String?,

    /**
     * CSRF Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "CSRF Token",
        example = "a1b2c3d4e5f6g7h8"
    )
    val csrfToken: String
)
