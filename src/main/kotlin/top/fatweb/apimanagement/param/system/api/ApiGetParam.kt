package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get API parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 查询请求参数")
data class ApiGetParam(
    /**
     * API code to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 API 编码", example = "api:v1:avatar:getRandom")
    var searchCode: String?,

    /**
     * API name to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 API 名称", example = "随机头像")
    var searchName: String?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "启用状态",
        allowableValues = ["true", "false"],
        defaultValue = "true",
        example = "true"
    )
    var enabled: Boolean?
) : PageSortParam()
