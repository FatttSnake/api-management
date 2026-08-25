package top.fatweb.apimanagement.controller.permission

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.permission.*
import top.fatweb.apimanagement.service.permission.IAuthenticationService
import top.fatweb.apimanagement.vo.permission.LoginVo
import top.fatweb.apimanagement.vo.permission.RegisterVo
import top.fatweb.apimanagement.vo.permission.TokenVo
import top.fatweb.apimanagement.vo.permission.TwoFactorVo

/**
 * Authentication controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IAuthenticationService
 */
@BaseController(name = "身份认证", description = "身份认证相关接口")
class AuthenticationController(
    private val authenticationService: IAuthenticationService
) {
    /**
     * Register
     *
     * @see request Request information
     * @see response Response information
     * @param registerParam Register parameters
     * @return Response object includes user ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see RegisterParam
     * @see ResponseResult
     * @see RegisterVo
     */
    @Operation(summary = "注册")
    @PostMapping("/register")
    fun register(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @Valid @RequestBody registerParam: RegisterParam
    ): ResponseResult<RegisterVo> = ResponseResult.success(
        code = ResponseCode.PERMISSION_REGISTER_SUCCESS,
        data = authenticationService.register(
            request = request,
            response = response,
            registerParam = registerParam
        )
    )


    /**
     * Send verify email
     *
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "发送验证邮件")
    @PostMapping("/resend")
    fun resend(): ResponseResult<Unit> {
        authenticationService.resend()

        return ResponseResult.success(ResponseCode.PERMISSION_RESEND_SUCCESS)
    }

    /**
     * Verify email
     *
     * @param verifyParam Verify parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see VerifyParam
     * @see ResponseResult
     */
    @Operation(summary = "验证邮箱")
    @PostMapping("/verify")
    fun verify(@ProcessParam @Valid @RequestBody verifyParam: VerifyParam): ResponseResult<Unit> {
        authenticationService.verify(verifyParam)

        return ResponseResult.success(ResponseCode.PERMISSION_VERIFY_SUCCESS)
    }

    /**
     * Forget password
     *
     * @param request Request information
     * @param forgetParam Forget parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see ForgetParam
     * @see ResponseResult
     */
    @Operation(summary = "忘记密码")
    @PostMapping("/forget")
    fun forget(request: HttpServletRequest, @Valid @RequestBody forgetParam: ForgetParam): ResponseResult<Unit> {
        authenticationService.forget(
            request = request,
            forgetParam = forgetParam
        )

        return ResponseResult.success(ResponseCode.PERMISSION_FORGET_SUCCESS)
    }

    /**
     * Retrieve password
     *
     * @param request Request information
     * @param retrieveParam Retrieve parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see RetrieveParam
     * @see ResponseResult
     */
    @Operation(summary = "找回密码")
    @PostMapping("/retrieve")
    fun retrieve(
        request: HttpServletRequest,
        @Valid @RequestBody retrieveParam: RetrieveParam
    ): ResponseResult<Unit> {
        authenticationService.retrieve(
            request = request,
            retrieveParam = retrieveParam
        )

        return ResponseResult.success(ResponseCode.PERMISSION_RETRIEVE_SUCCESS)
    }

    /**
     * Login
     *
     * @param request Request information
     * @param response Response information
     * @param loginParam Login parameters
     * @return Response object includes login information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see LoginParam
     * @see ResponseResult
     * @see LoginVo
     */
    @Operation(summary = "登录")
    @PostMapping("/login")
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @ProcessParam @Valid @RequestBody loginParam: LoginParam
    ): ResponseResult<LoginVo> =
        ResponseResult.success(
            code = ResponseCode.PERMISSION_LOGIN_SUCCESS,
            msg = "Login success",
            data = authenticationService.login(
                request = request,
                response = response,
                loginParam = loginParam
            )
        )

    /**
     * Create two-factor
     *
     * @return Response object includes two-factor QR code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see TwoFactorVo
     */
    @Operation(summary = "创建双因素验证码")
    @GetMapping("/two-factor")
    fun createTwoFactor(): ResponseResult<TwoFactorVo> =
        ResponseResult.success(data = authenticationService.createTwoFactor())

    /**
     * Validate two-factor
     *
     * @param twoFactorValidateParam Validate two-factor parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorValidateParam
     * @see ResponseResult
     */
    @Operation(summary = "验证双因素")
    @PostMapping("/two-factor")
    fun validateTwoFactor(@RequestBody @Valid twoFactorValidateParam: TwoFactorValidateParam): ResponseResult<Unit> =
        if (authenticationService.validateTwoFactor(twoFactorValidateParam)) ResponseResult.success()
        else ResponseResult.fail()

    /**
     * Remove two-factor
     *
     * @param twoFactorRemoveParam Remove two-factor parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorRemoveParam
     * @see ResponseResult
     */
    @Operation(summary = "移除双因素")
    @DeleteMapping("/two-factor")
    fun removeTwoFactor(@RequestBody @Valid twoFactorRemoveParam: TwoFactorRemoveParam): ResponseResult<Unit> =
        if (authenticationService.removeTwoFactor(twoFactorRemoveParam)) ResponseResult.success()
        else ResponseResult.fail()


    /**
     * Logout
     *
     * @param request Request information
     * @param response Response information
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see ResponseResult
     */
    @Operation(summary = "登出")
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseResult<Unit> =
        when (authenticationService.logout(request = request, response = response)) {
            true -> ResponseResult.success(
                code = ResponseCode.PERMISSION_LOGOUT_SUCCESS,
                msg = "Logout success",
                data = null
            )

            false -> ResponseResult.fail(
                code = ResponseCode.PERMISSION_LOGOUT_FAILED,
                msg = "Logout failed",
                data = null
            )
        }

    /**
     * Refresh token
     *
     * @param request Request information
     * @param response Response information
     * @param cookieRefreshToken Refresh token in cookie
     * @param bodyRefreshToken Refresh token in body
     * @param csrfToken CSRF Token
     * @return Response object includes new token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see ResponseResult
     * @see TokenVo
     */
    @Operation(summary = "更新 Token")
    @PostMapping("/token")
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @CookieValue("refresh_token") cookieRefreshToken: String?,
        @RequestBody bodyRefreshToken: String?,
        @RequestHeader("X-CSRF-TOKEN", required = false) csrfToken: String?
    ): ResponseResult<TokenVo> = ResponseResult.success(
        code = ResponseCode.PERMISSION_TOKEN_REFRESH_SUCCESS,
        msg = "Token refresh success",
        data = authenticationService.refreshToken(
            request = request,
            response = response,
            refreshToken = cookieRefreshToken ?: bodyRefreshToken,
            csrfToken = csrfToken
        )
    )
}
