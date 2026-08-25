package top.fatweb.apimanagement.service.permission

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import top.fatweb.apimanagement.param.permission.*
import top.fatweb.apimanagement.vo.permission.LoginVo
import top.fatweb.apimanagement.vo.permission.RegisterVo
import top.fatweb.apimanagement.vo.permission.TokenVo
import top.fatweb.apimanagement.vo.permission.TwoFactorVo

/**
 * Authentication service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IAuthenticationService {
    /**
     * Register
     *
     * @param request Request information
     * @param response Response information
     * @param registerParam Register parameters
     * @return RegisterVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see RegisterParam
     * @see RegisterVo
     */
    fun register(request: HttpServletRequest, response: HttpServletResponse, registerParam: RegisterParam): RegisterVo

    /**
     * Send verify email
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resend()

    /**
     * Verify email
     *
     * @param verifyParam Verify parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see VerifyParam
     */
    fun verify(verifyParam: VerifyParam)

    /**
     * Forget password
     *
     * @param request Request information
     * @param forgetParam Forget parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see ForgetParam
     */
    fun forget(request: HttpServletRequest, forgetParam: ForgetParam)

    /**
     * Retrieve password
     *
     * @param request Request information
     * @param retrieveParam Retrieve parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see RetrieveParam
     */
    fun retrieve(request: HttpServletRequest, retrieveParam: RetrieveParam)

    /**
     * Login
     *
     * @param request Request information
     * @param response Response information
     * @param loginParam Login parameters
     * @return LoginVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see LoginParam
     * @see LoginVo
     */
    fun login(request: HttpServletRequest, response: HttpServletResponse, loginParam: LoginParam): LoginVo

    /**
     * Create two-factor
     *
     * @return Two-factor QR code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorVo
     */
    fun createTwoFactor(): TwoFactorVo

    /**
     * Validate two-factor
     *
     * @param twoFactorValidateParam Validate two-factor parameters
     * @return Result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorValidateParam
     */
    fun validateTwoFactor(twoFactorValidateParam: TwoFactorValidateParam): Boolean

    /**
     * Remove two-factor
     *
     * @param twoFactorRemoveParam Remove two-factor parameters
     * @return Result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorRemoveParam
     */
    fun removeTwoFactor(twoFactorRemoveParam: TwoFactorRemoveParam): Boolean

    /**
     * Logout
     *
     * @param request Request information
     * @param response Response information
     * @return Logout result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     */
    fun logout(request: HttpServletRequest, response: HttpServletResponse): Boolean

    /**
     * Refresh token
     *
     * @param request Request information
     * @param response Response information
     * @param refreshToken Refresh token
     * @param csrfToken CSRF token for cross-origin protection
     * @return TokenVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HttpServletRequest
     * @see HttpServletResponse
     * @see TokenVo
     */
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
        refreshToken: String?,
        csrfToken: String? = null
    ): TokenVo
}
