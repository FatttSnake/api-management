package top.fatweb.apimanagement.vo.permission

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer

/**
 * Register value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "注册返回参数")
data class RegisterVo(
    /**
     * Refresh Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "Refresh Token",
        example = "eyJhbGciOiJIUzI1NiJ9.e30.signature",
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
        example = "eyJhbGciOiJIUzI1NiJ9.e30.signature",
    )
    val accessToken: String,
    /**
     * User ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "User ID", example = "1709986058679975938")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: Long?,
    /**
     * CSRF Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "CSRF Token",
        example = "a1b2c3d4e5f6g7h8",
    )
    val csrfToken: String,
)
