package top.fatweb.apimanagement.param.permission.role

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

/**
 * Delete role parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "角色删除请求参数")
data class RoleDeleteParam(
    /**
     * List of role IDs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "角色 ID 列表", required = true)
    @field:NotEmpty(message = "Ids can not be empty")
    val ids: List<Long>?
)
