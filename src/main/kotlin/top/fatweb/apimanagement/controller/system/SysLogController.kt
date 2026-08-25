package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.SysLogGetParam
import top.fatweb.apimanagement.service.system.ISysLogService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.SysLogVo

/**
 * System log viewer controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ISysLogService
 */
@BaseController(path = ["/system/log"], name = "系统日志", description = "系统日志相关接口")
class SysLogController(
    private val sysLogService: ISysLogService
) {
    /**
     * Get system log in page
     *
     * @param sysLogGetParam Get system log parameters
     * @return Response object includes system log in page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SysLogGetParam
     * @see ResponseResult
     * @see SysLogVo
     */
    @Operation(summary = "获取")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:log:all:query')")
    fun get(@ProcessParam @Valid sysLogGetParam: SysLogGetParam?): ResponseResult<PageVo<SysLogVo>> {
        return ResponseResult.success(
            ResponseCode.DATABASE_SELECT_SUCCESS, data = sysLogService.getPage(sysLogGetParam)
        )
    }
}
