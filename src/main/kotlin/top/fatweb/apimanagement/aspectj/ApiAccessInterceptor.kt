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
import top.fatweb.apimanagement.component.ratelimit.RateLimitDimension
import top.fatweb.apimanagement.component.ratelimit.RateLimitHeaders
import top.fatweb.apimanagement.component.ratelimit.RateLimitPolicy
import top.fatweb.apimanagement.component.ratelimit.RateLimitResult
import top.fatweb.apimanagement.component.ratelimit.RateLimiter
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
    private val rateLimitPolicy: RateLimitPolicy,
    private val rateLimiter: RateLimiter,
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
            response = response,
            plugin = plugin,
            api = apiInterface,
            pluginId = pluginId,
            classLoader = handler.classLoader,
        )
    }

    private fun admit(
        request: HttpServletRequest,
        response: HttpServletResponse,
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
                    response = response,
                    plugin = plugin,
                    api = api,
                    principal = principal,
                )

            is LoginUser ->
                admitAccount(
                    request = request,
                    response = response,
                    plugin = plugin,
                    api = api,
                    principal = principal,
                )

            else -> {
                if (api.needKey == 1) {
                    throw ApiKeyRequiredException()
                }
                admitAnonymous(
                    request = request,
                    response = response,
                    plugin = plugin,
                    api = api,
                )
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
                this.requestIp = getRequestIp(request, serverProperties.security)
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
        response: HttpServletResponse,
        plugin: ApiPlugin,
        api: ApiInterface,
        principal: ApiKeyPrincipal,
    ) {
        if (api.needKey == 1 && !principal.permissions.contains(api.code)) {
            throw ApiKeyPermissionDeniedException()
        }

        request.setAttribute(ATTR_API_KEY_ID, principal.keyId)
        request.setAttribute(ATTR_USER_ID, principal.userId)

        applyApiKeyRateLimits(request = request, response = response, plugin = plugin, api = api, principal = principal)
        checkKeyQuota(principal = principal, api = api)
        principal.userId?.let { checkUserQuota(userId = it, api = api) }
        applyApiKeyBilling(request = request, plugin = plugin, api = api, principal = principal)
    }

    private fun applyApiKeyRateLimits(
        request: HttpServletRequest,
        response: HttpServletResponse,
        plugin: ApiPlugin,
        api: ApiInterface,
        principal: ApiKeyPrincipal,
    ) {
        val decisions = mutableListOf<RateLimitResult>()
        // Key first: a request rejected by its own key limit must not consume the user's
        // aggregate, or one throttled key would throttle every other key of the same user.
        principal.keyId?.let {
            acquireOrReject(
                request = request,
                plugin = plugin,
                api = api,
                dimension = RateLimitDimension.KEY_TOTAL,
                scopeId = it.toString(),
                limit = rateLimitPolicy.keyTotal(principal),
                decisions = decisions,
            )
        }
        principal.userId?.let {
            acquireOrReject(
                request = request,
                plugin = plugin,
                api = api,
                dimension = RateLimitDimension.USER_TOTAL,
                scopeId = it.toString(),
                limit = rateLimitPolicy.userTotal(),
                decisions = decisions,
            )
        }
        api.id?.let {
            acquireOrReject(
                request = request,
                plugin = plugin,
                api = api,
                dimension = RateLimitDimension.API_TOTAL,
                scopeId = it.toString(),
                limit = rateLimitPolicy.apiTotal(api, plugin),
                decisions = decisions,
            )
        }
        applyTightestHeaders(response = response, decisions = decisions)
    }

    private fun applyApiKeyBilling(
        request: HttpServletRequest,
        plugin: ApiPlugin,
        api: ApiInterface,
        principal: ApiKeyPrincipal,
    ) {
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

    private fun admitAccount(
        request: HttpServletRequest,
        response: HttpServletResponse,
        plugin: ApiPlugin,
        api: ApiInterface,
        principal: LoginUser,
    ) {
        val userId = principal.user.id ?: throw ApiAccountNotFoundException()
        request.setAttribute(ATTR_USER_ID, userId)

        checkAccountAccess(plugin = plugin, api = api, principal = principal)
        applyAccountRateLimits(request = request, response = response, plugin = plugin, api = api, userId = userId)
        checkUserQuota(userId = userId, api = api)
        applyAccountBilling(request = request, plugin = plugin, api = api, userId = userId)
    }

    private fun checkAccountAccess(plugin: ApiPlugin, api: ApiInterface, principal: LoginUser) {
        // Access mode: RESTRICTED requires the caller to hold the api:* operation code
        val accessMode = api.accessMode ?: plugin.defaultAccessMode ?: ApiInterface.AccessMode.RESTRICTED
        if (accessMode != ApiInterface.AccessMode.RESTRICTED) {
            return
        }

        val codes =
            principal.user.operations
                ?.mapNotNull { it.code }
                ?.toSet() ?: emptySet()
        if (api.code !in codes) {
            throw ApiPermissionDeniedException()
        }
    }

    private fun applyAccountRateLimits(
        request: HttpServletRequest,
        response: HttpServletResponse,
        plugin: ApiPlugin,
        api: ApiInterface,
        userId: Long,
    ) {
        val decisions = mutableListOf<RateLimitResult>()
        acquireOrReject(
            request = request,
            plugin = plugin,
            api = api,
            dimension = RateLimitDimension.USER_TOTAL,
            scopeId = userId.toString(),
            limit = rateLimitPolicy.userTotal(),
            decisions = decisions,
        )
        api.id?.let {
            acquireOrReject(
                request = request,
                plugin = plugin,
                api = api,
                dimension = RateLimitDimension.API_TOTAL,
                scopeId = it.toString(),
                limit = rateLimitPolicy.apiTotal(api, plugin),
                decisions = decisions,
            )
        }
        applyTightestHeaders(response = response, decisions = decisions)
    }

    private fun applyAccountBilling(request: HttpServletRequest, plugin: ApiPlugin, api: ApiInterface, userId: Long) {
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

    private fun admitAnonymous(
        request: HttpServletRequest,
        response: HttpServletResponse,
        plugin: ApiPlugin,
        api: ApiInterface,
    ) {
        request.setAttribute(ATTR_COST, BigDecimal.ZERO)
        request.setAttribute(ATTR_BILLING_MODE, ApiInterface.BillingMode.FREE)

        val decisions = mutableListOf<RateLimitResult>()
        val ip = getRequestIp(request, serverProperties.security)
        acquireOrReject(
            request = request,
            plugin = plugin,
            api = api,
            dimension = RateLimitDimension.ANONYMOUS_IP,
            scopeId = ip,
            limit = rateLimitPolicy.anonymousIpTotal(),
            decisions = decisions,
        )
        api.id?.let {
            acquireOrReject(
                request = request,
                plugin = plugin,
                api = api,
                dimension = RateLimitDimension.API_TOTAL,
                scopeId = it.toString(),
                limit = rateLimitPolicy.apiTotal(api, plugin),
                decisions = decisions,
            )
        }
        applyTightestHeaders(response = response, decisions = decisions)
    }

    private fun acquireOrReject(
        request: HttpServletRequest,
        plugin: ApiPlugin,
        api: ApiInterface,
        dimension: RateLimitDimension,
        scopeId: String,
        limit: Int,
        decisions: MutableList<RateLimitResult>,
    ) {
        val result = rateLimiter.acquire(dimension, scopeId, limit)
        decisions += result
        if (result.allowed) {
            return
        }

        recordRateLimitRejection(api)
        logger.debug(
            "Rate limit hit: dimension={} plugin={} api={} scope={} ip={} limit={} remaining={} retryAfter={}",
            dimension.segment,
            plugin.pluginId,
            api.code,
            scopeId,
            getRequestIp(request, serverProperties.security),
            result.limit,
            result.remaining,
            result.retryAfterSeconds,
        )
        throw RequestTooFrequentException(
            scope = dimension.segment,
            limit = result.limit,
            remaining = result.remaining,
            retryAfterSeconds = result.retryAfterSeconds,
        )
    }

    private fun applyTightestHeaders(response: HttpServletResponse, decisions: List<RateLimitResult>) {
        decisions
            .filter { it.limit > 0 }
            .minByOrNull { it.remaining }
            ?.let { RateLimitHeaders.apply(response, it) }
    }

    private fun checkUserQuota(userId: Long, api: ApiInterface) {
        val limit = rateLimitPolicy.userQuota()
        val period = rateLimitPolicy.userQuotaPeriod()
        if (limit <= 0 || period <= 0) {
            return
        }

        val periodIdx = Instant.now().epochSecond / period
        val count =
            redisProvider.increment(
                key = "${serverProperties.security.tokenIssuer}_user_quota:$userId:$periodIdx",
                delta = 1,
                ttlSeconds = period.toLong(),
            )
        if (count > limit) {
            logger.debug("User quota exceeded: user={} api={} limit={} count={}", userId, api.code, limit, count)
            throw QuotaExceededException()
        }
    }

    private fun checkKeyQuota(principal: ApiKeyPrincipal, api: ApiInterface) {
        val limit = rateLimitPolicy.keyQuota(principal)
        if (limit <= 0) {
            return
        }
        val period = rateLimitPolicy.keyQuotaPeriod(principal)
        if (period <= 0) {
            return
        }

        val keyId = principal.keyId ?: return
        val periodIdx = Instant.now().epochSecond / period
        val count =
            redisProvider.increment(
                key = "${serverProperties.security.tokenIssuer}_key_quota:$keyId:$periodIdx",
                delta = 1,
                ttlSeconds = period.toLong(),
            )
        if (count > limit) {
            logger.debug("Key quota exceeded: key={} api={} limit={} count={}", keyId, api.code, limit, count)
            throw QuotaExceededException()
        }
    }

    private fun balanceCheckEnabled(): Boolean = SettingsOperator.getValue(ApiSettings::balanceCheckEnable, true)

    private fun recordRateLimitRejection(api: ApiInterface) {
        val code = api.code ?: return
        runCatching {
            redisProvider.increment(
                key = "${serverProperties.security.tokenIssuer}_apimetrics_ratelimited:$code",
                delta = 1,
                ttlSeconds = METRICS_WINDOW_SECONDS,
            )
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
