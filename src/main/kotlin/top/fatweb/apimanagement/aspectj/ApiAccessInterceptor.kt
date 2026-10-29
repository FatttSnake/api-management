package top.fatweb.apimanagement.aspectj

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor
import top.fatweb.apimanagement.component.api.ApiPath
import top.fatweb.apimanagement.component.plugin.PluginInFlightTracker
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.exception.ApiAccountNotFoundException
import top.fatweb.apimanagement.exception.ApiDisabledException
import top.fatweb.apimanagement.exception.ApiKeyPermissionDeniedException
import top.fatweb.apimanagement.exception.ApiKeyRequiredException
import top.fatweb.apimanagement.exception.ApiPermissionDeniedException
import top.fatweb.apimanagement.exception.InsufficientBalanceException
import top.fatweb.apimanagement.exception.QuotaExceededException
import top.fatweb.apimanagement.exception.RequestTooFrequentException
import top.fatweb.apimanagement.plugin.runtime.route.PluginInvocation
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiTransactionService
import top.fatweb.apimanagement.service.api.IApiUsageService
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
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class ApiAccessInterceptor(
    @param:Qualifier("applicationTaskExecutor") private val customThreadPoolTaskExecutor: Executor,
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val pluginRegistryCache: PluginRegistryCache,
    private val apiUsageService: IApiUsageService,
    private val apiAccountService: IApiAccountService,
    private val apiTransactionService: IApiTransactionService,
    private val pluginInFlightTracker: PluginInFlightTracker,
) : AsyncHandlerInterceptor {
    private val logger = LoggerFactory.getLogger(ApiAccessInterceptor::class.java)

    private companion object {
        const val ATTR_API = "apimanagement.api"
        const val ATTR_INFLIGHT = "apimanagement.inflight"
        const val ATTR_API_KEY_ID = "apimanagement.apiKeyId"
        const val ATTR_USER_ID = "apimanagement.userId"
        const val ATTR_COST = "apimanagement.cost"
        const val ATTR_BILLING_MODE = "apimanagement.billingMode"
        const val ATTR_ALREADY_DEDUCTED = "apimanagement.alreadyDeducted"
        const val ATTR_START = "apimanagement.startNanos"
        const val ATTR_TCCL = "apimanagement.pluginContextClassLoader"
        const val METRICS_WINDOW_SECONDS = 120L
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is PluginInvocation) {
            // A switched-off plugin has no routes. Its requests land on the static-resource
            // handler; answer them as "disabled" rather than as a wrong URL, when the URL
            // belongs to an installed plugin.
            val scoped = ApiPath.parse(request) ?: return true
            val plugin = pluginRegistryCache.getByPluginId(scoped.plugin) ?: return true
            if (plugin.enable != 1) {
                throw ApiDisabledException()
            }
            return true
        }

        val apiInterface = pluginRegistryCache.getByCode(handler.apiCode) ?: throw ApiDisabledException()
        val pluginId = apiInterface.pluginId ?: throw ApiDisabledException()
        val plugin = pluginRegistryCache.getByPluginId(pluginId) ?: throw ApiDisabledException()
        if (apiInterface.enable != 1 || plugin.enable != 1) {
            throw ApiDisabledException()
        }

        return admit(
            request = request,
            plugin = plugin,
            api = apiInterface,
            pluginId = pluginId,
            classLoader = handler.classLoader,
        )
    }

    private fun admit(
        request: HttpServletRequest,
        plugin: ApiPlugin,
        api: ApiInterface,
        pluginId: String,
        classLoader: ClassLoader,
    ): Boolean {
        request.setAttribute(ATTR_API, api)
        request.setAttribute(ATTR_START, System.nanoTime())

        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is ApiKeyPrincipal ->
                admitApiKey(
                    request = request,
                    plugin = plugin,
                    api = api,
                    principal = principal,
                )

            is LoginUser ->
                admitAccount(
                    request = request,
                    plugin = plugin,
                    api = api,
                    principal = principal,
                )

            else -> {
                if (api.needKey == 1) {
                    throw ApiKeyRequiredException()
                }
                request.setAttribute(ATTR_COST, BigDecimal.ZERO)
                request.setAttribute(ATTR_BILLING_MODE, ApiInterface.BillingMode.FREE)
            }
        }

        // Entered last, and only on the paths that actually reach the plugin: an unmount waits
        // for the requests counted here before it closes the plugin's resources, so a request
        // counted by anything above this point would be waited for after being refused. Spring
        // calls afterCompletion only for requests this method admitted, which is what pairs
        // every entry with an exit.
        val handle = pluginInFlightTracker.tryEnter(pluginId) ?: throw ApiDisabledException()
        request.setAttribute(ATTR_INFLIGHT, handle)

        // Set last, after every point above that can still refuse the request: Spring calls
        // afterCompletion only for a request this method admitted, so a class loader installed
        // before a throw would have nothing to restore it. The handler invocation runs between
        // here and afterCompletion on this same thread.
        request.setAttribute(ATTR_TCCL, Thread.currentThread().contextClassLoader)
        Thread.currentThread().contextClassLoader = classLoader

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        // Restored first, before anything else can return early: the thread carries a plugin's
        // class loader only for the span of its own request, and the pool hands it on afterwards
        restoreTccl(request)

        // Left before anything else can return early: the counter has to come back down on
        // every admitted request, including one that failed before the API attributes were
        // read. ATTR_INFLIGHT rather than ATTR_API marks it, because ATTR_API is set before
        // admission can still refuse the request.
        (request.getAttribute(ATTR_INFLIGHT) as? PluginInFlightTracker.Handle)?.let {
            pluginInFlightTracker.exit(it)
        }

        val api = request.getAttribute(ATTR_API) as? ApiInterface ?: return
        val startNanos = request.getAttribute(ATTR_START) as? Long ?: return
        val success = ex == null
        val executeTime = (System.nanoTime() - startNanos) / 1_000_000
        val apiKeyId = request.getAttribute(ATTR_API_KEY_ID) as? Long
        val userId = request.getAttribute(ATTR_USER_ID) as? Long
        val cost = request.getAttribute(ATTR_COST) as? BigDecimal ?: BigDecimal.ZERO
        val billingMode =
            request.getAttribute(ATTR_BILLING_MODE) as? ApiInterface.BillingMode ?: ApiInterface.BillingMode.FREE
        val alreadyDeducted = request.getAttribute(ATTR_ALREADY_DEDUCTED) as? Boolean ?: false

        recordMetrics(api = api, success = success, executeTime = executeTime, apiKeyId = apiKeyId)

        val usage =
            ApiUsage().apply {
                this.apiKeyId = apiKeyId
                this.apiId = api.id
                this.apiCode = api.code
                this.userId = userId
                this.requestPath = request.requestURI
                this.requestMethod = request.method
                this.responseCode = response.status
                this.success = if (success) 1 else 0
                this.executeTime = executeTime
                this.requestIp = getRequestIp(request, serverProperties.security.trustProxyHeaders)
                this.traceId = TraceIdUtil.get()
                this.cost = cost
                this.billingMode = billingMode
            }

        val shouldCharge =
            cost.signum() > 0 &&
                when (billingMode) {
                    ApiInterface.BillingMode.ALWAYS -> true
                    ApiInterface.BillingMode.SUCCESS_ONLY -> success
                    else -> false
                }

        if (!shouldCharge) {
            customThreadPoolTaskExecutor.execute(SaveUsageThread(usage = usage, apiUsageService = apiUsageService))
            return
        }

        customThreadPoolTaskExecutor.execute {
            val usageId =
                try {
                    apiUsageService.saveUsage(usage)
                } catch (ex: Exception) {
                    logger.error("Cannot record usage for API {} of user {}", usage.apiCode, usage.userId, ex)
                    return@execute
                }
            usage.id = usageId
            val ownerId = usage.userId ?: return@execute
            val deducted = alreadyDeducted || apiAccountService.deduct(userId = ownerId, cost = cost)
            if (deducted) {
                val balanceAfter = apiAccountService.getBalance(ownerId)
                try {
                    apiTransactionService.saveDeduct(apiUsage = usage, balanceAfter = balanceAfter)
                } catch (ex: Exception) {
                    logger.error(
                        "Cannot record a deduction of {} for user {} (usage {})",
                        cost,
                        ownerId,
                        usageId,
                        ex,
                    )
                }
            }
        }
    }

    override fun afterConcurrentHandlingStarted(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ) {
        // An async handler returns to the container before afterCompletion runs on the dispatch
        // thread. Restore here so the pool never keeps a plugin's class loader.
        restoreTccl(request)
    }

    private fun restoreTccl(request: HttpServletRequest) {
        (request.getAttribute(ATTR_TCCL) as? ClassLoader)?.let {
            Thread.currentThread().contextClassLoader = it
            request.removeAttribute(ATTR_TCCL)
        }
    }

    private fun admitApiKey(
        request: HttpServletRequest,
        plugin: ApiPlugin,
        api: ApiInterface,
        principal: ApiKeyPrincipal,
    ) {
        if (api.needKey == 1 && !principal.permissions.contains(api.code)) {
            throw ApiKeyPermissionDeniedException()
        }

        request.setAttribute(ATTR_API_KEY_ID, principal.keyId)
        request.setAttribute(ATTR_USER_ID, principal.userId)

        checkRateLimit(principal = principal, plugin = plugin, api = api)
        checkQuota(principal)

        val cost = api.price ?: plugin.defaultPrice ?: BigDecimal.ZERO
        val billingMode = api.billingMode ?: ApiInterface.BillingMode.SUCCESS_ONLY
        request.setAttribute(ATTR_COST, cost)
        request.setAttribute(ATTR_BILLING_MODE, billingMode)

        when (billingMode) {
            ApiInterface.BillingMode.FREE -> Unit
            ApiInterface.BillingMode.ALWAYS -> {
                val ownerId = principal.userId ?: throw ApiAccountNotFoundException()
                if (!apiAccountService.deduct(userId = ownerId, cost = cost)) {
                    throw InsufficientBalanceException()
                }
                request.setAttribute(ATTR_ALREADY_DEDUCTED, true)
            }

            ApiInterface.BillingMode.SUCCESS_ONLY -> {
                val ownerId = principal.userId ?: throw ApiAccountNotFoundException()
                if (balanceCheckEnabled() && !apiAccountService.checkBalance(userId = ownerId, cost = cost)) {
                    throw InsufficientBalanceException()
                }
            }
        }
    }

    private fun admitAccount(request: HttpServletRequest, plugin: ApiPlugin, api: ApiInterface, principal: LoginUser) {
        val userId = principal.user.id ?: throw ApiAccountNotFoundException()
        request.setAttribute(ATTR_USER_ID, userId)

        // Access mode: RESTRICTED requires the caller to hold the api:* operation code
        val accessMode = api.accessMode ?: plugin.defaultAccessMode ?: ApiInterface.AccessMode.RESTRICTED
        if (accessMode == ApiInterface.AccessMode.RESTRICTED) {
            val codes =
                principal.user.operations
                    ?.mapNotNull { it.code }
                    ?.toSet() ?: emptySet()
            if (api.code !in codes) {
                throw ApiPermissionDeniedException()
            }
        }

        // Account/JWT callers share the per-API global rate limit with API keys
        checkApiGlobalRateLimit(plugin = plugin, api = api)

        // Built-in super admin (user id 0) is exempt from billing but still recorded in usage
        if (userId == 0L) {
            request.setAttribute(ATTR_COST, BigDecimal.ZERO)
            request.setAttribute(ATTR_BILLING_MODE, ApiInterface.BillingMode.FREE)
            return
        }

        val cost = api.price ?: plugin.defaultPrice ?: BigDecimal.ZERO
        val billingMode = api.billingMode ?: ApiInterface.BillingMode.SUCCESS_ONLY
        request.setAttribute(ATTR_COST, cost)
        request.setAttribute(ATTR_BILLING_MODE, billingMode)

        when (billingMode) {
            ApiInterface.BillingMode.FREE -> Unit
            ApiInterface.BillingMode.ALWAYS -> {
                if (!apiAccountService.deduct(userId = userId, cost = cost)) {
                    throw InsufficientBalanceException()
                }
                request.setAttribute(ATTR_ALREADY_DEDUCTED, true)
            }

            ApiInterface.BillingMode.SUCCESS_ONLY -> {
                if (balanceCheckEnabled() && !apiAccountService.checkBalance(userId = userId, cost = cost)) {
                    throw InsufficientBalanceException()
                }
            }
        }
    }

    private fun balanceCheckEnabled(): Boolean = SettingsOperator.getValue(ApiSettings::balanceCheckEnable, true)

    private fun checkRateLimit(principal: ApiKeyPrincipal, plugin: ApiPlugin, api: ApiInterface) {
        val epochMinute = Instant.now().epochSecond / 60
        val keyId = principal.keyId ?: return
        val apiId = api.id ?: return

        val keyLimit = principal.rateLimit ?: SettingsOperator.getValue(ApiSettings::defaultRateLimitPerMin, 0)
        if (keyLimit > 0) {
            val count =
                redisProvider.increment(
                    key = "${serverProperties.security.tokenIssuer}_apikey_rl:$keyId:$apiId:$epochMinute",
                    delta = 1,
                    ttlSeconds = 60,
                )
            if (count > keyLimit) {
                throw RequestTooFrequentException()
            }
        }

        checkApiGlobalRateLimit(plugin = plugin, api = api)
    }

    private fun checkApiGlobalRateLimit(plugin: ApiPlugin, api: ApiInterface) {
        val epochMinute = Instant.now().epochSecond / 60
        val apiId = api.id ?: return
        val apiLimit = api.rateLimit ?: plugin.defaultRateLimit ?: 0
        if (apiLimit > 0) {
            val count =
                redisProvider.increment(
                    key = "${serverProperties.security.tokenIssuer}_apikey_rl:$apiId:global:$epochMinute",
                    delta = 1,
                    ttlSeconds = 60,
                )
            if (count > apiLimit) {
                throw RequestTooFrequentException()
            }
        }
    }

    private fun checkQuota(principal: ApiKeyPrincipal) {
        val quotaLimit = principal.quota ?: SettingsOperator.getValue(ApiSettings::defaultQuota, 0)
        if (quotaLimit <= 0) {
            return
        }
        val periodSeconds =
            (
                principal.quotaPeriod
                    ?: SettingsOperator.getValue(ApiSettings::defaultQuotaPeriodSeconds, 86400)
                ).toLong()
        if (periodSeconds <= 0) {
            return
        }
        val keyId = principal.keyId ?: return
        val periodIdx = Instant.now().epochSecond / periodSeconds
        val count =
            redisProvider.increment(
                key = "${serverProperties.security.tokenIssuer}_apikey_quota:$keyId:$periodIdx",
                delta = 1,
                ttlSeconds = periodSeconds,
            )
        if (count > quotaLimit) {
            throw QuotaExceededException()
        }
    }

    private fun recordMetrics(api: ApiInterface, success: Boolean, executeTime: Long, apiKeyId: Long?) {
        val code = api.code ?: return
        val prefix = "${serverProperties.security.tokenIssuer}_apimetrics"
        redisProvider.increment(
            key = "${prefix}_count:$code",
            delta = 1,
            ttlSeconds = METRICS_WINDOW_SECONDS,
        )
        if (!success) {
            redisProvider.increment(
                key = "${prefix}_error:$code",
                delta = 1,
                ttlSeconds = METRICS_WINDOW_SECONDS,
            )
        }
        if (executeTime > 0) {
            redisProvider.increment(
                key = "${prefix}_latency:$code",
                delta = executeTime,
                ttlSeconds = METRICS_WINDOW_SECONDS,
            )
        }
        apiKeyId?.let { redisProvider.setAdd("${prefix}_keys", it, ttlSeconds = METRICS_WINDOW_SECONDS) }
    }

    private class SaveUsageThread(val usage: ApiUsage, val apiUsageService: IApiUsageService) : Thread() {
        override fun run() {
            runCatching { apiUsageService.saveUsage(usage) }
        }
    }
}
