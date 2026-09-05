package top.fatweb.apimanagement.vo.permission

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.vo.permission.base.MenuVo
import top.fatweb.apimanagement.vo.permission.base.ModuleVo
import top.fatweb.apimanagement.vo.permission.base.OperationVo
import top.fatweb.apimanagement.vo.permission.base.ScopeVo

/**
 * Set of power value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "权限集合返回参数")
data class PowerSetVo(
    /**
     * List of ModuleVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ModuleVo
     */
    @field:Schema(description = "模块列表")
    val moduleList: List<ModuleVo>?,

    /**
     * List of MenuVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MenuVo
     */
    @field:Schema(description = "菜单列表")
    val menuList: List<MenuVo>?,

    /**
     * List of ScopeVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ScopeVo
     */
    @field:Schema(description = "作用域列表")
    val scopeList: List<ScopeVo>?,

    /**
     * List of OperationVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see OperationVo
     */
    @field:Schema(description = "操作列表")
    val operationList: List<OperationVo>?
)
