package top.fatweb.apimanagement.vo.permission.base

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Menu value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "菜单返回参数")
data class MenuVo(
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
    @field:Schema(description = "菜单名", example = "System")
    val name: String?,

    /**
     * URL
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "URL", example = "/system")
    val url: String?,

    /**
     * Parent ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "父 ID")
    val parentId: Long?,

    /**
     * Module ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "模块 ID")
    val moduleId: Long?
)
