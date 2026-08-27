package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get API interface parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 接口查询请求参数")
data class ApiInterfaceGetParam(
    /**
     * API code to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 API 编码", example = "api:avatar:v1:getRandom")
    var searchCode: String?,

    /**
     * API name to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 API 名称", example = "getRandom")
    var searchName: String?,

    /**
     * Owning plugin ID to filter by
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属插件 ID", example = "avatar")
    var pluginId: String?,

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
