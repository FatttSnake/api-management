package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.ApiGetParam
import top.fatweb.apimanagement.param.system.api.ApiUpdateParam
import top.fatweb.apimanagement.service.system.IApiService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiVo

/**
 * API registry management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiService
 */
@BaseController(path = ["/system/api"], name = "API 管理", description = "API 注册表管理相关接口")
class ApiRegistryController(
    private val apiService: IApiService
) {
    /**
     * Get API paging information
     *
     * @param apiGetParam Get API parameters
     * @return Response object includes API paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiVo
     */
    @Operation(summary = "获取 API 列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:api:registry:query')")
    fun get(@ProcessParam @Valid apiGetParam: ApiGetParam?): ResponseResult<PageVo<ApiVo>> =
        ResponseResult.databaseSuccess(data = apiService.getPage(apiGetParam))

    /**
     * Update API configuration
     *
     * @param apiUpdateParam Update API parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:api:registry:price')")
    fun update(@ProcessParam @Valid @RequestBody apiUpdateParam: ApiUpdateParam): ResponseResult<Unit> {
        apiService.update(apiUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }
}
