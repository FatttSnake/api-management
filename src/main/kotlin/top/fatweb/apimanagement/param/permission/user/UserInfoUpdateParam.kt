package top.fatweb.apimanagement.param.permission.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update user information parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "更新用户信息请求参数")
data class UserInfoUpdateParam(
    /**
     * Avatar base64
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "头像")
    val avatar: String?,

    /**
     * Nickname
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "昵称", example = "QwQ")
    @field:Size(min = 3, max = 20, message = "Nickname must be 3-20 characters")
    var nickname: String?
)
