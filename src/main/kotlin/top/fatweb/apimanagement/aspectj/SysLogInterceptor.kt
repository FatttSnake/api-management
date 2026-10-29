package top.fatweb.apimanagement.aspectj

import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import top.fatweb.apimanagement.component.security.JwtProvider
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.entity.system.SysLog
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.ISysLogService
import top.fatweb.apimanagement.util.TraceIdUtil
import top.fatweb.apimanagement.util.getApiKeyPrincipal
import top.fatweb.apimanagement.util.getLoginUserId
import top.fatweb.apimanagement.util.getRequestIp
import top.fatweb.apimanagement.vo.permission.LoginVo
import top.fatweb.apimanagement.vo.permission.TokenVo
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.StringJoiner
import java.util.concurrent.Executor

/**
 * System log interceptor
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Hidden
 */
@Hidden
@ControllerAdvice
class SysLogInterceptor(
    @param:Qualifier("applicationTaskExecutor") private val customThreadPoolTaskExecutor: Executor,
    private val serverProperties: ServerProperties,
    private val jwtProvider: JwtProvider,
    private val sysLogService: ISysLogService,
) : HandlerInterceptor,
    ResponseBodyAdvice<Any> {
    private val sysLogThreadLocal = ThreadLocal<SysLog>()
    private val resultThreadLocal = ThreadLocal<Any>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val sysLog =
            SysLog().apply {
                traceId = TraceIdUtil.get()
                operateUserId = getLoginUserId() ?: getApiKeyPrincipal()?.userId ?: -1
                apiKeyId = getApiKeyPrincipal()?.keyId
                startTime = LocalDateTime.now(ZoneOffset.UTC)
                requestUri = URI(request.requestURI).path
                requestParams = formatParams(request.parameterMap)
                requestMethod = request.method
                requestIp = getRequestIp(request, serverProperties.security.trustProxyHeaders)
                requestServerAddress = "${request.scheme}://${request.serverName}:${request.serverPort}"
                userAgent = request.getHeader("User-Agent")
            }

        sysLogThreadLocal.set(sysLog)

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val sysLog = sysLogThreadLocal.get()
        val result = resultThreadLocal.get()
        sysLog.endTime = LocalDateTime.now(ZoneOffset.UTC)
        sysLog.executeTime = ChronoUnit.MILLIS.between(sysLog.startTime, sysLog.endTime)
        if (result is ResponseResult<*>) {
            if (result.success) {
                sysLog.apply {
                    logType = requestUri?.let {
                        when {
                            it.startsWith("/login") -> SysLog.LogType.LOGIN
                            it.startsWith("/logout") -> SysLog.LogType.LOGOUT
                            it.startsWith("/register") -> SysLog.LogType.REGISTER
                            it.startsWith("/system/statistics/") -> SysLog.LogType.STATISTICS
                            else -> SysLog.LogType.INFO
                        }
                    } ?: SysLog.LogType.INFO
                    exception = 0
                }
                if (result.data is LoginVo) {
                    sysLog.operateUserId = result.data.userId ?: -1
                }
                if (result.data is TokenVo) {
                    try {
                        val jwt = jwtProvider.parseJwt(result.data.refreshToken)
                        sysLog.operateUserId = jwt.subject.toLong()
                    } catch (_: Exception) {
                    }
                }
            } else {
                sysLog.apply {
                    logType = SysLog.LogType.ERROR
                    exception = 1
                    exceptionInfo = result.msg
                }
            }

            customThreadPoolTaskExecutor.execute {
                sysLog.operateTime = LocalDateTime.now(ZoneOffset.UTC)
                sysLogService.save(sysLog)
            }
        }
        sysLogThreadLocal.remove()
        resultThreadLocal.remove()
    }

    private fun formatParams(parameterMap: Map<String, Array<String>>): String {
        val params = StringJoiner("&")

        parameterMap.forEach { (key, values) ->
            val value =
                if (isSensitive(key)) {
                    "*"
                } else {
                    values.joinToString(",").take(MAX_PARAM_VALUE_LENGTH)
                }
            params.add("$key=$value")
        }

        return params.toString()
    }

    private fun isSensitive(key: String): Boolean = SENSITIVE_KEYWORDS.any { key.contains(it, ignoreCase = true) }

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean =
        true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        resultThreadLocal.set(body)

        if (body is ResponseResult<*> && body.code == ResponseCode.SYSTEM_ERROR.code) {
            return ResponseResult.build(body.code, body.success, "fail", body.data)
        }

        return body
    }

    private companion object {
        const val MAX_PARAM_VALUE_LENGTH = 512

        val SENSITIVE_KEYWORDS =
            listOf(
                "password",
                "passcode",
                "secret",
                "token",
                "credential",
                "authorization",
                "captcha",
                "verification",
            )
    }
}
