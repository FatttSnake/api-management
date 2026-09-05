package top.fatweb.apimanagement.controller.user

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.converter.api.toDocVo
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.vo.api.ApiDocVo

/**
 * User-facing API documentation controller
 *
 * Serves callable API documentation to end users (list + per-plugin detail with
 * the embedded OpenAPI fragment) so the frontend can render it directly.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginService
 */
@BaseController(path = ["/user/api/docs"], name = "用户 API 文档", description = "用户可调用 API 文档")
class UserApiDocController(
    private val objectMapper: JsonMapper,
    private val apiPluginService: IApiPluginService
) {
    /**
     * Get the API documentation catalog of all installed plugins
     *
     * @return Response object includes plugin documentation list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiDocVo
     */
    @Operation(summary = "获取用户 API 文档列表")
    @GetMapping
    fun get(): ResponseResult<List<ApiDocVo>> {
        val interfacesByPlugin = apiPluginService.listEnabledInterfaces().groupBy { it.pluginId }
        val vos = apiPluginService.list().map { plugin ->
            plugin.toDocVo(
                interfaces = interfacesByPlugin[plugin.pluginId].orEmpty().map(ApiInterface::toVo),
                openapi = null
            )
        }

        return ResponseResult.databaseSuccess(data = vos)
    }

    /**
     * Get the API documentation detail of one plugin
     *
     * @param pluginId Plugin ID
     * @return Response object includes plugin documentation detail
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiDocVo
     */
    @Operation(summary = "获取插件 API 文档详情")
    @GetMapping("/{pluginId}")
    fun getDetail(@PathVariable pluginId: String): ResponseResult<ApiDocVo> {
        val plugin = apiPluginService.getByPluginId(pluginId) ?: throw NoRecordFoundException()
        val interfaces = apiPluginService.listEnabledInterfaces()
            .filter { it.pluginId == pluginId }
            .map(ApiInterface::toVo)
        val openapi = plugin.openapi?.let { runCatching { objectMapper.readTree(it) }.getOrNull() }

        return ResponseResult.databaseSuccess(data = plugin.toDocVo(interfaces, openapi))
    }
}
