package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiUsage.ApiUsageGetParam
import top.fatweb.apimanagement.service.api.IApiUsageService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiUsageVo

/**
 * API usage management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiUsageService
 */
@BaseController(path = ["/system/api/usage"], name = "API 用量", description = "API 用量查询相关接口")
class ApiUsageController(
    private val apiUsageService: IApiUsageService
) {
    /**
     * Get API usage paging information
     *
     * @param apiUsageGetParam Get API usage parameters
     * @return Response object includes usage paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUsageGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiUsageVo
     */
    @Operation(summary = "获取 API 用量")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:operations:usage:query')")
    fun get(@ProcessParam @Valid apiUsageGetParam: ApiUsageGetParam?): ResponseResult<PageVo<ApiUsageVo>> =
        ResponseResult.databaseSuccess(data = apiUsageService.getPage(true, apiUsageGetParam))
}
