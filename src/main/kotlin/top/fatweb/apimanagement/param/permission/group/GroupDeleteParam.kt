package top.fatweb.apimanagement.param.permission.group

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

/**
 * Delete group parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "用户组删除请求参数")
data class GroupDeleteParam(
    /**
     * List of group IDs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户组 ID 列表", required = true)
    @field:NotEmpty(message = "Ids can not be empty")
    val ids: List<Long>?
)
