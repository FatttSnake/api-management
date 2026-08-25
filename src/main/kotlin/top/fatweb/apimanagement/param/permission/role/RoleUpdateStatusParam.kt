package top.fatweb.apimanagement.param.permission.role

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * Update status of role parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "角色更改状态请求参数")
data class RoleUpdateStatusParam(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "角色 ID", required = true)
    @field:NotNull(message = "Role id can not be null")
    val id: Long?,

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", allowableValues = ["true", "false"], defaultValue = "true", example = "true")
    val enable: Boolean = true
)
