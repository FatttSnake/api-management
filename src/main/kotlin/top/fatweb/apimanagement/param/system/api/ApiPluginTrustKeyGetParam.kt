package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get API plugin trust key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 插件信任密钥查询请求参数")
data class ApiPluginTrustKeyGetParam(
    /**
     * Plugin trust key alias to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询密钥别名", example = "master")
    var searchAlias: String?,

    /**
     * Enable status
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
    var enable: Boolean?
) : PageSortParam()
