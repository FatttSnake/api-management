package top.fatweb.apimanagement.controller.user

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiAccount.ApiTopUpParam
import top.fatweb.apimanagement.param.system.apiAccount.ApiTransactionGetParam
import top.fatweb.apimanagement.service.system.IApiAccountService
import top.fatweb.apimanagement.service.system.IApiTransactionService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiAccountVo
import top.fatweb.apimanagement.vo.system.ApiTransactionVo

/**
 * User-facing API account self-service controller
 *
 * Any authenticated user manages only their own account.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiAccountService
 * @see IApiTransactionService
 */
@BaseController(path = ["/user/api/account"], name = "我的 API 账户", description = "用户自助 API 账户接口")
class UserApiAccountController(
    private val apiAccountService: IApiAccountService,
    private val apiTransactionService: IApiTransactionService
) {
    /**
     * Get my API account
     *
     * @return Response object includes account information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiAccountVo
     */
    @Operation(summary = "获取我的 API 账户")
    @GetMapping
    fun get(): ResponseResult<ApiAccountVo> =
        ResponseResult.databaseSuccess(data = apiAccountService.getAccount(false, null))

    /**
     * Get my API transaction paging information
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
    @Operation(summary = "获取我的 API 流水")
    @GetMapping("/transactions")
    fun getTransactions(
        @ProcessParam @Valid apiTransactionGetParam: ApiTransactionGetParam?
    ): ResponseResult<PageVo<ApiTransactionVo>> =
        ResponseResult.databaseSuccess(
            data = apiTransactionService.getPage(false, apiTransactionGetParam)
        )
}
