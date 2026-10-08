package top.fatweb.apimanagement.controller.user

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.ApiDocGetParam
import top.fatweb.apimanagement.service.api.IApiDocService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiDocVo

/**
 * User-facing API documentation controller
 *
 * Serves callable API documentation to end users (list + per-plugin detail with the
 * embedded OpenAPI fragment) so the frontend can render it directly.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiDocService
 */
@BaseController(path = ["/user/api/docs"], name = "用户 API 文档", description = "用户可调用 API 文档")
class UserApiDocController(
    private val apiDocService: IApiDocService
) {
    /**
     * Get the API documentation catalog of the plugins I can call
     *
     * @param apiDocGetParam Get API doc parameters
     * @return Response object includes plugin documentation paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiDocGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiDocVo
     */
    @Operation(summary = "获取用户 API 文档列表")
    @GetMapping
    fun get(@ProcessParam @Valid apiDocGetParam: ApiDocGetParam?): ResponseResult<PageVo<ApiDocVo>> =
        ResponseResult.databaseSuccess(data = apiDocService.getPage(apiDocGetParam))

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
    fun getDetail(@PathVariable pluginId: String): ResponseResult<ApiDocVo> =
        ResponseResult.databaseSuccess(data = apiDocService.getDetail(pluginId))
}
