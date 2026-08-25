package top.fatweb.apimanagement.param.permission.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * Update password of user parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "更改用户密码请求参数")
data class UserUpdatePasswordParam(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户 ID", required = true)
    @field:NotNull(message = "ID can not be null")
    val id: Long?,

    /**
     * New password
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "新密码", required = true)
    @field:NotBlank(message = "Password can not be blank")
    val password: String?,

    /**
     * Credentials expiration time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "认证过期时间", example = "1900-01-01T00:00:00.000Z")
    val credentialsExpiration: LocalDateTime?
)
