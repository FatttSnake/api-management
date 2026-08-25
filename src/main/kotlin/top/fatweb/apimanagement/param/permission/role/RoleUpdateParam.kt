package top.fatweb.apimanagement.param.permission.role

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update role parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "角色更新请求参数")
data class RoleUpdateParam(
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
     * Name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "角色名称", required = true, example = "Role_1")
    @field:NotBlank(message = "Name can not be blank")
    var name: String?,

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", allowableValues = ["true", "false"], defaultValue = "true", example = "true")
    val enable: Boolean = true,

    /**
     * List of power IDs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "权限 ID 列表")
    val powerIds: List<Long>?
)
