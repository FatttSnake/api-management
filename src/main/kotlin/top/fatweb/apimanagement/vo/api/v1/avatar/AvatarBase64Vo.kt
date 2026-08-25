package top.fatweb.apimanagement.vo.api.v1.avatar

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Avatar base64 value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "头像 Base64 返回参数")
data class AvatarBase64Vo(
    /**
     * Base64
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "base64")
    val base64: String?
)
