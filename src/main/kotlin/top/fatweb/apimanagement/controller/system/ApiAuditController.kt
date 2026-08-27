package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiAudit.ApiAuditGetParam
import top.fatweb.apimanagement.service.system.IApiAuditService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiAuditVo

/**
 * API audit controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiAuditService
 */
@BaseController(path = ["/system/api/audit"], name = "API 审计", description = "API 审计查询相关接口")
class ApiAuditController(
    private val apiAuditService: IApiAuditService
) {
    /**
     * Get API audit paging information
     *
     * @param apiAuditGetParam Get API audit parameters
     * @return Response object includes audit paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiAuditGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiAuditVo
     */
    @Operation(summary = "获取 API 审计")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:api:audit:query')")
    fun get(@ProcessParam @Valid apiAuditGetParam: ApiAuditGetParam?): ResponseResult<PageVo<ApiAuditVo>> =
        ResponseResult.databaseSuccess(data = apiAuditService.getPage(apiAuditGetParam))
}
