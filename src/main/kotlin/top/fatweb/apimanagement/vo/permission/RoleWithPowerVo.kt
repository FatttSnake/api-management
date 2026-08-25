package top.fatweb.apimanagement.vo.permission

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.vo.permission.base.ScopeVo
import top.fatweb.apimanagement.vo.permission.base.MenuVo
import top.fatweb.apimanagement.vo.permission.base.ModuleVo
import top.fatweb.apimanagement.vo.permission.base.OperationVo
import java.time.LocalDateTime

/**
 * Role with power value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "角色返回参数")
data class RoleWithPowerVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "角色名", example = "Role")
    val name: String?,

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    val enable: Boolean?,

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "创建时间", example = "1900-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?,

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "修改时间", example = "1900-01-01T00:00:00.000Z")
    val updateTime: LocalDateTime?,

    /**
     * List of ModuleVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ModuleVo
     */
    @field:Schema(description = "模块列表")
    val modules: List<ModuleVo>?,

    /**
     * List of MenuVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MenuVo
     */
    @field:Schema(description = "菜单列表")
    val menus: List<MenuVo>?,

    /**
     * List of ScopeVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ScopeVo
     */
    @field:Schema(description = "作用域列表")
    val scopes: List<ScopeVo>?,

    /**
     * List of OperationVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see OperationVo
     */
    @field:Schema(description = "操作列表")
    val operations: List<OperationVo>?
)
