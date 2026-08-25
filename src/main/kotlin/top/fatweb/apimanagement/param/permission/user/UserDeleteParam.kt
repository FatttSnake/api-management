package top.fatweb.apimanagement.param.permission.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

/**
 * Delete user parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "删除用户请求参数")
data class UserDeleteParam(
    /**
     * List of user IDs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户 ID 列表", required = true)
    @field:NotEmpty(message = "Ids can not be empty")
    val ids: List<Long>?
)
