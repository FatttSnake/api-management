package top.fatweb.apimanagement.controller.permission

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.service.permission.IPowerService
import top.fatweb.apimanagement.vo.permission.PowerSetVo

/**
 * Power management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPowerService
 */
@BaseController(path = ["/system/power"], name = "权限管理", description = "权限管理相关接口")
class PowerController(
    private val powerService: IPowerService
) {
    /**
     * Get power list
     *
     * @return Response object includes power list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see PowerSetVo
     */
    @Operation(summary = "获取权限列表")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('system:power:all:list', 'system:role:one:add', 'system:role:one:modify')")
    fun getList(): ResponseResult<PowerSetVo> = ResponseResult.databaseSuccess(data = powerService.getList())
}
