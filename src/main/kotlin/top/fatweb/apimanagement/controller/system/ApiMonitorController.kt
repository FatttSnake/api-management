package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.service.api.IApiMonitorService
import top.fatweb.apimanagement.vo.api.ApiMonitorDashboardVo

/**
 * API monitor controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiMonitorService
 */
@BaseController(path = ["/system/api/monitor"], name = "API 监控", description = "API 监控相关接口")
class ApiMonitorController(
    private val apiMonitorService: IApiMonitorService
) {
    /**
     * Get API monitor dashboard
     *
     * @return Response object includes dashboard information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiMonitorDashboardVo
     */
    @Operation(summary = "获取 API 监控看板")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('system:operations:monitor:dashboard')")
    fun dashboard(): ResponseResult<ApiMonitorDashboardVo> =
        ResponseResult.databaseSuccess(data = apiMonitorService.dashboard())
}
