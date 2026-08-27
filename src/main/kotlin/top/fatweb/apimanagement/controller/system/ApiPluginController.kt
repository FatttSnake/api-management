package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.service.system.IApiPluginService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiInterfaceVo
import top.fatweb.apimanagement.vo.system.ApiPluginVo

/**
 * API plugin management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginService
 */
@BaseController(path = ["/system/api"], name = "API 插件管理", description = "API 插件管理相关接口")
class ApiPluginController(
    private val apiPluginService: IApiPluginService
) {
    /**
     * Get API plugin paging information
     *
     * @param apiPluginGetParam Get API plugin parameters
     * @return Response object includes API plugin paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiPluginVo
     */
    @Operation(summary = "获取 API 插件列表")
    @GetMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:api:registry:query')")
    fun getPlugin(@ProcessParam @Valid apiPluginGetParam: ApiPluginGetParam?): ResponseResult<PageVo<ApiPluginVo>> =
        ResponseResult.databaseSuccess(data = apiPluginService.getPluginPage(apiPluginGetParam))

    /**
     * Get API interface paging information
     *
     * @param apiInterfaceGetParam Get API interface parameters
     * @return Response object includes API interface paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiInterfaceVo
     */
    @Operation(summary = "获取 API 接口列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:api:registry:query')")
    fun get(@ProcessParam @Valid apiInterfaceGetParam: ApiInterfaceGetParam?): ResponseResult<PageVo<ApiInterfaceVo>> =
        ResponseResult.databaseSuccess(data = apiPluginService.getInterfacePage(apiInterfaceGetParam))

    /**
     * Update API plugin
     *
     * @param apiPluginUpdateParam Update API plugin parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 插件")
    @PutMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:api:registry:price')")
    fun updatePlugin(@ProcessParam @Valid @RequestBody apiPluginUpdateParam: ApiPluginUpdateParam): ResponseResult<Unit> {
        apiPluginService.updatePlugin(apiPluginUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update API interface configuration
     *
     * @param apiInterfaceUpdateParam Update API interface parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 接口配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:api:registry:price')")
    fun update(@ProcessParam @Valid @RequestBody apiInterfaceUpdateParam: ApiInterfaceUpdateParam): ResponseResult<Unit> {
        apiPluginService.updateInterface(apiInterfaceUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }
}
