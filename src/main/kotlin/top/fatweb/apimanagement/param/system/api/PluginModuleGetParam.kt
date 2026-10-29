package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get plugin module parameters.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件依赖模块查询请求参数")
data class PluginModuleGetParam(
    /**
     * Maven group to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 Maven group")
    var searchGroup: String?,
    /**
     * Maven artifact to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 Maven artifact")
    var searchArtifact: String?,
    /**
     * Maven version to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 Maven version")
    var searchVersion: String?,
    /**
     * SHA-256 prefix to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询模块 SHA-256 前缀")
    var searchSha: String?,
    /**
     * Whether to list only referenced (`true`) or unreferenced (`false`) modules; null lists both
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否被插件引用，留空查询全部")
    var referenced: Boolean?,
) : PageSortParam()
