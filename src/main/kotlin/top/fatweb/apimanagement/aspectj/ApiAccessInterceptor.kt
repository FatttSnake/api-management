package top.fatweb.apimanagement.aspectj

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import top.fatweb.apimanagement.annotation.ApiController
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.entity.system.Api
import top.fatweb.apimanagement.entity.system.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.exception.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IApiAccountService
import top.fatweb.apimanagement.service.system.IApiService
import top.fatweb.apimanagement.service.system.IApiTransactionService
import top.fatweb.apimanagement.service.system.IApiUsageService
import top.fatweb.apimanagement.service.system.impl.ApiServiceImpl
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.util.TraceIdUtil
import top.fatweb.apimanagement.util.getRequestIp
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.Executor

/**
 * API access interceptor
 *
 * Admits API-key requests (permission subset, rate limit, quota, billing) and records usage.
 * Registered after SysLogInterceptor so rejected requests are still captured in the audit log.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IApiService
 * @see IApiUsageService
 * @see IApiAccountService
 * @see IApiTransactionService
 * @see HandlerInterceptor
 */
@Component
class ApiAccessInterceptor(
    @param:Qualifier("applicationTaskExecutor") private val customThreadPoolTaskExecutor: Executor,
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiService: IApiService,
    private val apiUsageService: IApiUsageService,
    private val apiAccountService: IApiAccountService,
    private val apiTransactionService: IApiTransactionService
) : HandlerInterceptor {
    private companion object {
        const val ATTR_API = "apimanagement.api"
        const val ATTR_API_KEY_ID = "apimanagement.apiKeyId"
        const val ATTR_USER_ID = "apimanagement.userId"
        const val ATTR_COST = "apimanagement.cost"
        const val ATTR_BILLING_MODE = "apimanagement.billingMode"
        const val ATTR_ALREADY_DEDUCTED = "apimanagement.alreadyDeducted"
        const val ATTR_START = "apimanagement.startNanos"
        const val METRICS_WINDOW_SECONDS = 120L
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }
        val annotation = handler.beanType.getAnnotation(ApiController::class.java) ?: return true
        val apiCode = ApiServiceImpl.buildCode(annotation, handler.method.name)
        val api = apiService.getByCode(apiCode) ?: throw ApiDisabledException()
        if (api.enabled != 1) {
            throw ApiDisabledException()
        }

        request.setAttribute(ATTR_API, api)
        request.setAttribute(ATTR_START, System.nanoTime())

        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is ApiKeyPrincipal -> admitApiKey(request, api, principal)
            is LoginUser -> {
                request.setAttribute(ATTR_USER_ID, principal.user.id)
                request.setAttribute(ATTR_COST, BigDecimal.ZERO)
                request.setAttribute(ATTR_BILLING_MODE, Api.BillingMode.FREE)
            }
            else -> {
                if (api.needKey == 1) {
                    throw ApiKeyRequiredException()
                }
                request.setAttribute(ATTR_COST, BigDecimal.ZERO)
                request.setAttribute(ATTR_BILLING_MODE, Api.BillingMode.FREE)
            }
        }

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?
    ) {
        val api = request.getAttribute(ATTR_API) as? Api ?: return
        val startNanos = request.getAttribute(ATTR_START) as? Long ?: return
        val success = ex == null
        val executeTime = (System.nanoTime() - startNanos) / 1_000_000
        val apiKeyId = request.getAttribute(ATTR_API_KEY_ID) as? Long
        val userId = request.getAttribute(ATTR_USER_ID) as? Long
        val cost = request.getAttribute(ATTR_COST) as? BigDecimal ?: BigDecimal.ZERO
        val billingMode = request.getAttribute(ATTR_BILLING_MODE) as? Api.BillingMode ?: Api.BillingMode.FREE
        val alreadyDeducted = request.getAttribute(ATTR_ALREADY_DEDUCTED) as? Boolean ?: false

        recordMetrics(api, success, executeTime, apiKeyId)

        val usage = ApiUsage().apply {
            this.apiKeyId = apiKeyId
            apiId = api.id
            apiCode = api.code
            this.userId = userId
            requestPath = request.requestURI
            requestMethod = request.method
            responseCode = response.status
            this.success = if (success) 1 else 0
            this.executeTime = executeTime
            requestIp = getRequestIp(request)
            traceId = TraceIdUtil.get()
            this.cost = cost
            this.billingMode = billingMode
        }

        val shouldCharge = cost.signum() > 0 && when (billingMode) {
            Api.BillingMode.ALWAYS -> true
            Api.BillingMode.SUCCESS_ONLY -> success
            else -> false
        }

        if (!shouldCharge) {
            customThreadPoolTaskExecutor.execute(SaveUsageThread(usage, apiUsageService))
            return
        }

        customThreadPoolTaskExecutor.execute {
            val usageId = runCatching { apiUsageService.saveUsage(usage) }.getOrNull() ?: return@execute
            usage.id = usageId
            val ownerId = usage.userId ?: return@execute
            val deducted = alreadyDeducted || apiAccountService.deduct(ownerId, cost)
            if (deducted) {
                val balanceAfter = apiAccountService.getBalance(ownerId)
                runCatching { apiTransactionService.saveDeduct(usage, balanceAfter) }
            }
        }
    }

    private fun admitApiKey(request: HttpServletRequest, api: Api, principal: ApiKeyPrincipal) {
        if (api.needKey == 1 && !principal.permissions.contains(api.code)) {
            throw ApiKeyPermissionDeniedException()
        }

        request.setAttribute(ATTR_API_KEY_ID, principal.keyId)
        request.setAttribute(ATTR_USER_ID, principal.userId)

        checkRateLimit(principal, api)
        checkQuota(principal, api)

        val cost = api.price ?: BigDecimal.ZERO
        val billingMode = api.billingMode ?: Api.BillingMode.SUCCESS_ONLY
        request.setAttribute(ATTR_COST, cost)
        request.setAttribute(ATTR_BILLING_MODE, billingMode)

        when (billingMode) {
            Api.BillingMode.FREE -> Unit
            Api.BillingMode.ALWAYS -> {
                val ownerId = principal.userId ?: throw ApiAccountNotFoundException()
                if (!apiAccountService.deduct(ownerId, cost)) {
                    throw InsufficientBalanceException()
                }
                request.setAttribute(ATTR_ALREADY_DEDUCTED, true)
            }
            Api.BillingMode.SUCCESS_ONLY -> {
                val ownerId = principal.userId ?: throw ApiAccountNotFoundException()
                if (!apiAccountService.checkBalance(ownerId, cost)) {
                    throw InsufficientBalanceException()
                }
            }
        }
    }

    private fun checkRateLimit(principal: ApiKeyPrincipal, api: Api) {
        val epochMinute = Instant.now().epochSecond / 60
        val keyId = principal.keyId ?: return
        val apiId = api.id ?: return

        val keyLimit = principal.rateLimit ?: SettingsOperator.getValue(ApiSettings::defaultRateLimitPerMin, 0)
        if (keyLimit > 0) {
            val count = redisProvider.increment("${serverProperties.security.tokenIssuer}_apikey_rl:$keyId:$apiId:$epochMinute", 1, 60)
            if (count > keyLimit) {
                throw RequestTooFrequentException()
            }
        }

        val apiLimit = api.rateLimit ?: 0
        if (apiLimit > 0) {
            val count = redisProvider.increment("${serverProperties.security.tokenIssuer}_apikey_rl:$apiId:global:$epochMinute", 1, 60)
            if (count > apiLimit) {
                throw RequestTooFrequentException()
            }
        }
    }

    private fun checkQuota(principal: ApiKeyPrincipal, api: Api) {
        val quotaLimit = principal.quota ?: SettingsOperator.getValue(ApiSettings::defaultQuota, 0)
        if (quotaLimit <= 0) {
            return
        }
        val periodSeconds = (principal.quotaPeriod
            ?: SettingsOperator.getValue(ApiSettings::defaultQuotaPeriodSeconds, 86400)).toLong()
        if (periodSeconds <= 0) {
            return
        }
        val keyId = principal.keyId ?: return
        val periodIdx = Instant.now().epochSecond / periodSeconds
        val count = redisProvider.increment(
            "${serverProperties.security.tokenIssuer}_apikey_quota:$keyId:$periodIdx", 1, periodSeconds
        )
        if (count > quotaLimit) {
            throw QuotaExceededException()
        }
    }

    private fun recordMetrics(api: Api, success: Boolean, executeTime: Long, apiKeyId: Long?) {
        val code = api.code ?: return
        val prefix = "${serverProperties.security.tokenIssuer}_apimetrics"
        redisProvider.increment("${prefix}_count:$code", 1, METRICS_WINDOW_SECONDS)
        if (!success) {
            redisProvider.increment("${prefix}_error:$code", 1, METRICS_WINDOW_SECONDS)
        }
        if (executeTime > 0) {
            redisProvider.increment("${prefix}_latency:$code", executeTime, METRICS_WINDOW_SECONDS)
        }
        apiKeyId?.let { redisProvider.setAdd("${prefix}_keys", it, ttlSeconds = METRICS_WINDOW_SECONDS) }
    }

    private class SaveUsageThread(
        val usage: ApiUsage,
        val apiUsageService: IApiUsageService
    ) : Thread() {
        override fun run() {
            runCatching { apiUsageService.saveUsage(usage) }
        }
    }
}
