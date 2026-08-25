package top.fatweb.apimanagement.vo.permission.base

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Module value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "模块返回参数")
data class ModuleVo(
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
    @field:Schema(description = "模块名", example = "系统")
    val name: String?
)
