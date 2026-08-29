package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.DefaultCsrfToken
import org.springframework.security.web.csrf.InvalidCsrfTokenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.component.security.CsrfTokenManager
import top.fatweb.apimanagement.component.security.JwtProvider
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.entity.permission.UserInfo
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.exception.*
import top.fatweb.apimanagement.http.TurnstileApi
import top.fatweb.apimanagement.param.permission.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.permission.IAuthenticationService
import top.fatweb.apimanagement.service.permission.IUserInfoService
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.service.system.ISensitiveWordService
import top.fatweb.apimanagement.settings.BaseSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.settings.TwoFactorSettings
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.permission.LoginVo
import top.fatweb.apimanagement.vo.permission.RegisterVo
import top.fatweb.apimanagement.vo.permission.TokenVo
import top.fatweb.apimanagement.vo.permission.TwoFactorVo
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Authentication service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see TemplateEngine
 * @see AuthenticationManager
 * @see PasswordEncoder
 * @see RedisProvider
 * @see JwtProvider
 * @see CsrfTokenManager
 * @see TurnstileApi
 * @see IUserService
 * @see IUserInfoService
 * @see ISensitiveWordService
 * @see IAuthenticationService
 */
@Service
class AuthenticationServiceImpl(
    private val serverProperties: ServerProperties,
    private val templateEngine: TemplateEngine,
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
    private val redisProvider: RedisProvider,
    private val jwtProvider: JwtProvider,
    private val csrfTokenManager: CsrfTokenManager,
    private val turnstileApi: TurnstileApi,
    private val userService: IUserService,
    private val userInfoService: IUserInfoService,
    private val sensitiveWordService: ISensitiveWordService
) : IAuthenticationService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @EventLogRecord(EventLog.Event.REGISTER)
    @Transactional
    override fun register(
        request: HttpServletRequest,
        response: HttpServletResponse,
        registerParam: RegisterParam
    ): RegisterVo {
        verifyCaptcha(registerParam.captchaCode, "register")
        sensitiveWordService.checkSensitiveWord(registerParam.username!!)

        val user = User().apply {
            username = registerParam.username
            password = passwordEncoder.encode(registerParam.password)
            verify =
                "${
                    LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli()
                }-${UUID.randomUUID()}-${UUID.randomUUID()}-${UUID.randomUUID()}"
            locking = 0
            enable = 1
        }
        saveOrThrowException { userService.save(user) }

        saveOrThrowException {
            userInfoService.save(UserInfo().apply {
                userId = user.id
                nickname = registerParam.username
                avatar = userService.generateAvatar(null)
                email = registerParam.email
            })
        }

        sendVerifyMail(user.username!!, user.verify!!, registerParam.email!!)

        val loginVo = login(
            request = request,
            response = response,
            account = registerParam.username!!,
            password = registerParam.password!!
        )

        return RegisterVo(
            refreshToken = loginVo.refreshToken,
            accessToken = loginVo.accessToken,
            userId = loginVo.userId,
            csrfToken = loginVo.csrfToken
        )
    }

    @Transactional
    override fun resend() {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getById(getLoginUserId()) }

        user.verify ?: throw NoVerificationRequiredException()

        if (LocalDateTime.ofInstant(Instant.ofEpochMilli(user.verify!!.split("-").first().toLong()), ZoneOffset.UTC)
                .isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
        ) {
            throw RequestTooFrequentException()
        }

        updateOrThrowException {
            userService.updateById(user.apply {
                verify = "${
                    LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli()
                }-${UUID.randomUUID()}-${UUID.randomUUID()}-${UUID.randomUUID()}"
                updateTime = LocalDateTime.now(ZoneOffset.UTC)
            })
        }

        getLoginUser()?.user?.userInfo?.email?.let {
            sendVerifyMail(user.username!!, user.verify!!, it)
        } ?: throw AccessDeniedException("Access Denied")
    }

    @EventLogRecord(EventLog.Event.VERIFY)
    @Transactional
    override fun verify(verifyParam: VerifyParam) {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getById(getLoginUserId()) }
        user.verify ?: throw NoVerificationRequiredException()
        if (LocalDateTime.ofInstant(Instant.ofEpochMilli(user.verify!!.split("-").first().toLong()), ZoneOffset.UTC)
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).minusHours(2)) || user.verify != verifyParam.code
        ) {
            throw VerificationCodeErrorOrExpiredException()
        }

        if (verifyParam.nickname.isNullOrBlank() || verifyParam.avatar.isNullOrBlank()) {
            throw AccountNeedInitException()
        }
        sensitiveWordService.checkSensitiveWord(verifyParam.nickname!!)

        updateOrThrowException {
            userService.update(
                KtUpdateWrapper(User()).eq(User::id, user.id).set(User::verify, null)
                    .set(User::updateTime, LocalDateTime.now(ZoneOffset.UTC))
            )
        }
        updateOrThrowException {
            userInfoService.update(
                KtUpdateWrapper(UserInfo()).eq(UserInfo::userId, user.id).set(UserInfo::nickname, verifyParam.nickname)
                    .set(UserInfo::avatar, verifyParam.avatar)
            )
        }
    }

    @Transactional
    override fun forget(request: HttpServletRequest, forgetParam: ForgetParam) {
        verifyCaptcha(forgetParam.captchaCode, "forget")

        val user = queryOrThrowException(UserNotFoundException()) {
            userService.getUserWithPowerByAccount(forgetParam.email!!)
        }

        user.forget?.let {
            if (LocalDateTime.ofInstant(Instant.ofEpochMilli(it.split("-").first().toLong()), ZoneOffset.UTC)
                    .isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
            ) {
                throw RequestTooFrequentException()
            }
        }

        val code = "${
            LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli()
        }-${UUID.randomUUID()}-${UUID.randomUUID()}-${UUID.randomUUID()}"
        updateOrThrowException {
            userService.update(
                KtUpdateWrapper(User())
                    .eq(User::id, user.id)
                    .set(User::forget, code)
            )
        }

        sendRetrieveMail(user.username!!, getRequestIp(request), code, forgetParam.email!!)
    }

    @Transactional
    override fun retrieve(request: HttpServletRequest, retrieveParam: RetrieveParam) {
        verifyCaptcha(retrieveParam.captchaCode, "retrieve")

        val codeStrings = retrieveParam.code!!.split("-")
        if (codeStrings.size != 16) {
            throw RetrieveCodeErrorOrExpiredException()
        }
        try {
            if (LocalDateTime.ofInstant(Instant.ofEpochMilli(codeStrings.first().toLong()), ZoneOffset.UTC)
                    .isBefore(LocalDateTime.now(ZoneOffset.UTC).minusHours(2))
            ) {
                throw RetrieveCodeErrorOrExpiredException()
            }
        } catch (_: Exception) {
            throw RetrieveCodeErrorOrExpiredException()
        }

        val user = queryOrThrowException(RetrieveCodeErrorOrExpiredException()) {
            userService.getOne(
                KtQueryWrapper(User())
                    .eq(User::forget, retrieveParam.code)
            )
        }
        val userInfo = queryOrThrowException {
            userInfoService.getOne(
                KtQueryWrapper(UserInfo())
                    .eq(UserInfo::userId, user.id)
            )
        }
        updateOrThrowException {
            userService.update(
                KtUpdateWrapper(User()).eq(User::id, user.id).set(User::forget, null)
                    .set(User::password, passwordEncoder.encode(retrieveParam.password!!))
            )
        }

        offlineUser(serverProperties = serverProperties, redisProvider = redisProvider, user.id!!)

        sendPasswordChangedMail(user.username!!, getRequestIp(request), userInfo.email!!)
    }

    @EventLogRecord(EventLog.Event.LOGIN)
    override fun login(request: HttpServletRequest, response: HttpServletResponse, loginParam: LoginParam): LoginVo {
        if (loginParam.twoFactorCode.isNullOrBlank()) {
            verifyCaptcha(loginParam.captchaCode, "login")
        }

        return login(
            request = request,
            response = response,
            account = loginParam.account!!,
            password = loginParam.password!!,
            twoFactorCode = loginParam.twoFactorCode
        )
    }

    override fun createTwoFactor(): TwoFactorVo {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getById(getLoginUserId()) }

        if (!user.twoFactor.isNullOrBlank() && !user.twoFactor!!.endsWith("?")) {
            throw AlreadyHasTwoFactorException()
        }

        val secretKey =
            TOTPUtil.generateSecretKey(SettingsOperator.getValue(TwoFactorSettings::secretKeyLength, 16))
        val qrCodeSVGBase64 = TOTPUtil.generateQRCodeSVGBase64(
            SettingsOperator.getValue(TwoFactorSettings::issuer, "ApiManagement"),
            user.username!!,
            secretKey
        )

        updateOrThrowException {
            userService.update(
                KtUpdateWrapper(User())
                    .eq(User::id, user.id)
                    .set(User::twoFactor, "${secretKey}?")
            )
        }

        return TwoFactorVo(qrCodeSVGBase64)
    }

    override fun validateTwoFactor(twoFactorValidateParam: TwoFactorValidateParam): Boolean {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getById(getLoginUserId()) }
        if (user.twoFactor.isNullOrBlank()) {
            throw NoTwoFactorFoundException()
        }
        if (!user.twoFactor!!.endsWith("?")) {
            throw AlreadyHasTwoFactorException()
        }
        val secretKey = user.twoFactor!!.substring(0, user.twoFactor!!.length - 1)

        if (TOTPUtil.validateCode(secretKey, twoFactorValidateParam.code!!)) {
            updateOrThrowException {
                userService.update(
                    KtUpdateWrapper(User())
                        .eq(User::id, user.id)
                        .set(User::twoFactor, secretKey)
                )
            }
            return true
        }

        return false
    }

    override fun removeTwoFactor(twoFactorRemoveParam: TwoFactorRemoveParam): Boolean {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getById(getLoginUserId()) }
        if (user.twoFactor.isNullOrBlank() || user.twoFactor!!.endsWith("?")) {
            throw NoTwoFactorFoundException()
        }

        if (TOTPUtil.validateCode(user.twoFactor!!, twoFactorRemoveParam.code!!)) {
            updateOrThrowException {
                userService.update(
                    KtUpdateWrapper(User())
                        .eq(User::id, user.id)
                        .set(User::twoFactor, null)
                )
            }
            return true
        }

        return false
    }

    @EventLogRecord(EventLog.Event.LOGOUT)
    override fun logout(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val token = getToken(serverProperties = serverProperties, request = request)

        var redisKeyPattern = "${serverProperties.security.tokenIssuer}_access_*:${token}"
        var redisKeys = redisProvider.keys(redisKeyPattern)
        if (redisKeys.isEmpty()) {
            return false
        }
        redisProvider.delObject(redisKeys)

        val refreshToken =
            Regex("${serverProperties.security.tokenIssuer}_access_.*?_(.*):.*").matchEntire(redisKeys.first())?.groupValues?.getOrNull(
                1
            )
        redisKeyPattern = "${serverProperties.security.tokenIssuer}_token_*:${refreshToken}"
        redisKeys = redisProvider.keys(redisKeyPattern)
        if (redisKeys.isEmpty()) {
            return false
        }
        redisProvider.delObject(redisKeys)

        csrfTokenManager.removeToken(getLoginUserId()!!, refreshToken!!)

        val cookie = Cookie("refresh_token", null).apply {
            isHttpOnly = true
            secure = true
            domain = request.serverName
            path = "/token"
            maxAge = 0
            setAttribute("SameSite", "None")
        }

        response.addCookie(cookie)

        return true
    }

    override fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
        refreshToken: String?,
        csrfToken: String?
    ): TokenVo {
        refreshToken ?: throw TokenRefreshErrorException()
        jwtProvider.parseJwt(refreshToken)

        var redisKeyPattern = "${serverProperties.security.tokenIssuer}_token_*:${refreshToken}"
        var redisKeys = redisProvider.keys(redisKeyPattern)
        if (redisKeys.isEmpty()) {
            throw TokenHasExpiredException()
        }

        val loginUser = redisProvider.getObject<LoginUser>(redisKeys.first()) ?: throw TokenHasExpiredException()
        val userId = loginUser.user.id!!

        if (csrfToken.isNullOrBlank() || !csrfTokenManager.validateToken(userId, refreshToken, csrfToken)) {
            throw InvalidCsrfTokenException(
                DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", csrfToken ?: ""),
                "CSRF token validation failed"
            )
        }

        val userIdStr = userId.toString()
        val newRefreshToken = jwtProvider.generateRefreshToken(userIdStr) ?: throw TokenRefreshErrorException()
        val newAccessToken = jwtProvider.generateAccessToken(userIdStr) ?: throw TokenRefreshErrorException()

        var redisKey = "${serverProperties.security.tokenIssuer}_token_${userIdStr}:${newRefreshToken}"
        redisProvider.setObject(
            key = redisKey,
            value = loginUser,
            timeout = serverProperties.security.refreshTokenTtl,
            timeUnit = serverProperties.security.refreshTokenTtlUnit
        )
        redisKey = "${serverProperties.security.tokenIssuer}_access_${userIdStr}_${newRefreshToken}:${newAccessToken}"
        redisProvider.setObject(
            key = redisKey,
            value = loginUser,
            timeout = serverProperties.security.accessTokenTtl,
            timeUnit = serverProperties.security.accessTokenTtlUnit
        )

        val cookie = Cookie("refresh_token", newRefreshToken).apply {
            isHttpOnly = true
            secure = true
            domain = request.serverName
            path = "/token"
            maxAge = serverProperties.security.refreshTokenTtlUnit.toSeconds(serverProperties.security.refreshTokenTtl)
                .toInt()
            setAttribute("SameSite", "None")
        }
        response.addCookie(cookie)

        redisProvider.delObject(redisKeys)
        redisKeyPattern = "${serverProperties.security.tokenIssuer}_access_*_${refreshToken}:*"
        redisKeys = redisProvider.keys(redisKeyPattern)
        redisProvider.delObject(redisKeys)

        csrfTokenManager.removeToken(userId, refreshToken)
        val newCsrfToken = csrfTokenManager.generateToken(userId, newRefreshToken)

        return TokenVo(
            refreshToken = newRefreshToken,
            accessToken = newAccessToken,
            csrfToken = newCsrfToken
        )
    }

    private fun sendVerifyMail(username: String, code: String, email: String) {
        val verifyUrl = UriComponentsBuilder
            .fromUriString(SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost"))
            .path("/verify")
            .queryParam("code", code)
            .build()
            .toUriString()
        val context = Context(
            Locale.getDefault(),
            mapOf(
                "systemName" to SettingsOperator.getValue(BaseSettings::systemName, "API Management"),
                "homeUrl" to SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost"),
                "username" to username,
                "verifyUrl" to verifyUrl
            )
        )
        val emailContent = templateEngine.process("email-verify-account-cn", context)
        MailUtil.sendSimpleMail(
            "验证您的账号", emailContent, true,
            email
        )
    }

    private fun sendRetrieveMail(username: String, ip: String, code: String, email: String) {
        val retrieveUrl = UriComponentsBuilder
            .fromUriString(SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost"))
            .path("/forget")
            .queryParam("code", code)
            .build()
            .toUriString()
        val context = Context(
            Locale.getDefault(),
            mapOf(
                "systemName" to SettingsOperator.getValue(BaseSettings::systemName, "API Management"),
                "homeUrl" to SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost"),
                "username" to username,
                "ipAddress" to ip,
                "retrieveUrl" to retrieveUrl
            )
        )
        val emailContent = templateEngine.process("email-retrieve-password-cn", context)
        MailUtil.sendSimpleMail(
            "找回您的密码", emailContent, true,
            email
        )
    }

    private fun sendPasswordChangedMail(username: String, ip: String, email: String) {
        val context = Context(
            Locale.getDefault(),
            mapOf(
                "systemName" to SettingsOperator.getValue(BaseSettings::systemName, "API Management"),
                "homeUrl" to SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost"),
                "username" to username,
                "ipAddress" to ip
            )
        )
        val emailContent = templateEngine.process("email-password-changed-cn", context)
        MailUtil.sendSimpleMail(
            "您的密码已更改", emailContent, true,
            email
        )
    }

    private fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        account: String,
        password: String,
        twoFactorCode: String? = null
    ): LoginVo {
        val usernamePasswordAuthenticationToken =
            UsernamePasswordAuthenticationToken(account, password)
        val authentication =
            authenticationManager.authenticate(usernamePasswordAuthenticationToken)

        val loginUser = authentication.principal as LoginUser
        loginUser.user.password = ""

        if (!loginUser.user.twoFactor.isNullOrBlank() && !loginUser.user.twoFactor!!.endsWith("?")) {
            if (twoFactorCode.isNullOrBlank()) {
                throw NeedTwoFactorException()
            }
            if (!TOTPUtil.validateCode(loginUser.user.twoFactor!!, twoFactorCode)) {
                throw TwoFactorVerificationCodeErrorException()
            }
        }

        logger.info("用户登录 [用户名: '{}', IP: '{}']", loginUser.username, getRequestIp(request))
        updateOrThrowException {
            userService.update(
                User().apply {
                    currentLoginIp = getRequestIp(request)
                    currentLoginTime = LocalDateTime.now(ZoneOffset.UTC)
                    lastLoginIp = loginUser.user.currentLoginIp
                    lastLoginTime = loginUser.user.currentLoginTime
                },
                KtUpdateWrapper(User())
                    .eq(User::username, loginUser.username)
            )
        }

        val userId = loginUser.user.id.toString()
        val refreshToken = jwtProvider.generateRefreshToken(userId) ?: throw LoginFailedException()
        val accessToken = jwtProvider.generateAccessToken(userId) ?: throw LoginFailedException()

        var redisKey = "${serverProperties.security.tokenIssuer}_token_${userId}:${refreshToken}"
        redisProvider.setObject(
            key = redisKey,
            value = loginUser,
            timeout = serverProperties.security.refreshTokenTtl,
            timeUnit = serverProperties.security.refreshTokenTtlUnit
        )
        redisKey = "${serverProperties.security.tokenIssuer}_access_${userId}_${refreshToken}:${accessToken}"
        redisProvider.setObject(
            key = redisKey,
            value = loginUser,
            timeout = serverProperties.security.accessTokenTtl,
            timeUnit = serverProperties.security.accessTokenTtlUnit
        )

        val cookie = Cookie("refresh_token", refreshToken).apply {
            isHttpOnly = true
            secure = true
            domain = request.serverName
            path = "/token"
            maxAge = serverProperties.security.refreshTokenTtlUnit.toSeconds(serverProperties.security.refreshTokenTtl)
                .toInt()
            setAttribute("SameSite", "None")
        }
        response.addCookie(cookie)

        val csrfToken = csrfTokenManager.generateToken(loginUser.user.id!!, refreshToken)

        return LoginVo(
            refreshToken = refreshToken,
            accessToken = accessToken,
            userId = loginUser.user.id,
            lastLoginTime = loginUser.user.currentLoginTime,
            lastLoginIp = loginUser.user.currentLoginIp,
            csrfToken = csrfToken
        )
    }

    private fun verifyCaptcha(captchaCode: String?, action: String? = null) {
        if (SettingsOperator.getValue(BaseSettings::turnstileSecretKey).isNullOrBlank()) {
            return
        }

        if (captchaCode.isNullOrBlank()) {
            throw InvalidCaptchaCodeException()
        }

        try {
            val siteverifyResponse =
                runBlocking {
                    turnstileApi.siteverify(
                        captchaCode,
                        SettingsOperator.getValue(BaseSettings::turnstileSecretKey) ?: ""
                    )
                }
            if (!siteverifyResponse.success || siteverifyResponse.action != action) {
                throw InvalidCaptchaCodeException()
            }
        } catch (e: Exception) {
            logger.error("Verify captcha error", e)
            throw InvalidCaptchaCodeException()
        }
    }
}
