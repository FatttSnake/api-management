package top.fatweb.apimanagement.controller.user

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiUsage.ApiUsageGetParam
import top.fatweb.apimanagement.service.system.IApiUsageService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiUsageVo

/**
 * User-facing API usage self-service controller
 *
 * Any authenticated user queries only their own usage.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiUsageService
 */
@BaseController(path = ["/user/api/usage"], name = "我的 API 用量", description = "用户自助 API 用量接口")
class UserApiUsageController(
    private val apiUsageService: IApiUsageService
) {
    /**
     * Get my API usage paging information
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
    @Operation(summary = "获取我的 API 用量")
    @GetMapping
    fun get(@ProcessParam @Valid apiUsageGetParam: ApiUsageGetParam?): ResponseResult<PageVo<ApiUsageVo>> =
        ResponseResult.databaseSuccess(data = apiUsageService.getPage(false, apiUsageGetParam))
}
