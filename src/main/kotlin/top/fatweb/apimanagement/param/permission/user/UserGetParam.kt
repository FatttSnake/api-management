package top.fatweb.apimanagement.param.permission.user

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get user parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "查询用户请求参数")
data class UserGetParam(
    /**
     * Type of search
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "搜索类型",
        allowableValues = ["ALL", "ID", "USERNAME", "NICKNAME", "EMAIL"],
        defaultValue = "ALL",
        example = "ALL"
    )
    val searchType: String = "ALL",

    /**
     * Value to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询内容", example = "User_1")
    var searchValue: String?,

    /**
     * Use regex
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "查询使用正则表达式",
        allowableValues = ["true", "false"],
        defaultValue = "false",
        example = "false"
    )
    val searchRegex: Boolean = false,
) : PageSortParam()
