package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiAccount.ApiTopUpParam
import top.fatweb.apimanagement.param.system.apiAccount.ApiTransactionGetParam
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiTransactionService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiAccountVo
import top.fatweb.apimanagement.vo.api.ApiTransactionVo

/**
 * API account management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiAccountService
 * @see IApiTransactionService
 */
@BaseController(path = ["/system/api/account"], name = "API 账户", description = "API 账户与流水相关接口")
class ApiAccountController(
    private val apiAccountService: IApiAccountService,
    private val apiTransactionService: IApiTransactionService
) {
    /**
     * Get API account of current user
     *
     * @return Response object includes account information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiAccountVo
     */
    @Operation(summary = "获取 API 账户")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:operations:account:query')")
    fun get(@RequestParam(required = false) userId: Long?): ResponseResult<ApiAccountVo> =
        ResponseResult.databaseSuccess(
            data = apiAccountService.getAccount(true, userId)
        )

    /**
     * Get API transaction paging information
     *
     * @param apiTransactionGetParam Get API transaction parameters
     * @return Response object includes transaction paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTransactionGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiTransactionVo
     */
    @Operation(summary = "获取 API 流水")
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyAuthority('system:operations:account:transactions')")
    fun getTransactions(
        @ProcessParam @Valid apiTransactionGetParam: ApiTransactionGetParam?
    ): ResponseResult<PageVo<ApiTransactionVo>> =
        ResponseResult.databaseSuccess(
            data = apiTransactionService.getPage(true, apiTransactionGetParam)
        )

    /**
     * Top up API account
     *
     * @param apiTopUpParam Top-up parameters
     * @return Response object includes transaction information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTopUpParam
     * @see ResponseResult
     * @see ApiTransactionVo
     */
    @Operation(summary = "API 账户充值")
    @PostMapping("/topup")
    @PreAuthorize("hasAnyAuthority('system:operations:account:topup')")
    fun topUp(@ProcessParam @Valid @RequestBody apiTopUpParam: ApiTopUpParam): ResponseResult<ApiTransactionVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.API_PLATFORM_KEY_TOPUP_SUCCESS,
            data = apiAccountService.topUp(true, apiTopUpParam)
        )
}
