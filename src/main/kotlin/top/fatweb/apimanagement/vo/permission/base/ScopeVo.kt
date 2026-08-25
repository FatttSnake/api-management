package top.fatweb.apimanagement.vo.permission.base

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Scope value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "作用域返回参数")
data class ScopeVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val id: Long?,

    /**
     * Name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "功能名", example = "AddButton")
    val name: String?,

    /**
     * Menu ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "菜单 ID")
    val menuId: Long?
)
