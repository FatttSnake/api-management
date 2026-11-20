package top.fatweb.apimanagement.aspectj

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import top.fatweb.apimanagement.api.v1.ApiHandler
import top.fatweb.apimanagement.api.v1.ApiResponse
import top.fatweb.apimanagement.api.v1.OperationMetadata
import top.fatweb.apimanagement.api.v1.RouteDefinition
import top.fatweb.apimanagement.component.plugin.PluginInFlightTracker
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.component.ratelimit.RateLimitDimension
import top.fatweb.apimanagement.component.ratelimit.RateLimitPolicy
import top.fatweb.apimanagement.component.ratelimit.RateLimitResult
import top.fatweb.apimanagement.component.ratelimit.RateLimiter
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.exception.QuotaExceededException
import top.fatweb.apimanagement.exception.RequestTooFrequentException
import top.fatweb.apimanagement.plugin.runtime.route.PluginInvocation
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute
import top.fatweb.apimanagement.plugin.runtime.route.RouteMatch
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiTransactionService
import top.fatweb.apimanagement.service.api.IApiUsageService
import java.math.BigDecimal
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * API access interceptor tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 * @see ApiAccessInterceptor
 */
class ApiAccessInterceptorTests {
    private lateinit var rateLimitPolicy: RateLimitPolicy
    private lateinit var redisProvider: RedisProvider
    private lateinit var limiter: RecordingRateLimiter
    private lateinit var interceptor: ApiAccessInterceptor
    private lateinit var api: ApiInterface
    private lateinit var plugin: ApiPlugin
    private lateinit var invocation: PluginInvocation
    private var originalTccl: ClassLoader? = null

    @BeforeEach
    fun setUp() {
        originalTccl = Thread.currentThread().contextClassLoader
        rateLimitPolicy = mock(RateLimitPolicy::class.java)
        redisProvider = mock(RedisProvider::class.java)
        limiter = RecordingRateLimiter()

        api =
            ApiInterface().apply {
                id = 1L
                pluginId = PLUGIN_ID
                code = API_CODE
                needKey = 1
                rateLimit = 100
                billingMode = ApiInterface.BillingMode.FREE
                accessMode = ApiInterface.AccessMode.DEFAULT
                enable = 1
            }
        plugin =
            ApiPlugin().apply {
                pluginId = PLUGIN_ID
                defaultRateLimit = null
                defaultPrice = null
                enable = 1
            }

        val definition = RouteDefinition("GET", "/ping", 1, OperationMetadata("ping"))
        val route = RegisteredRoute(PLUGIN_ID, definition, ApiHandler { ApiResponse.ok() }, javaClass.classLoader)
        invocation = PluginInvocation(RouteMatch(route, 1, emptyMap()))

        val registryCache = mock(PluginRegistryCache::class.java)
        Mockito.`when`(registryCache.getByCode(API_CODE)).thenReturn(api)
        Mockito.`when`(registryCache.getByPluginId(PLUGIN_ID)).thenReturn(plugin)

        interceptor =
            ApiAccessInterceptor(
                customThreadPoolTaskExecutor = Executor { it.run() },
                serverProperties = ServerProperties(),
                redisProvider = redisProvider,
                pluginRegistryCache = registryCache,
                apiUsageService = mock(IApiUsageService::class.java),
                apiAccountService = mock(IApiAccountService::class.java),
                apiTransactionService = mock(IApiTransactionService::class.java),
                pluginInFlightTracker = PluginInFlightTracker(),
                rateLimitPolicy = rateLimitPolicy,
                rateLimiter = limiter,
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        Thread.currentThread().contextClassLoader = originalTccl
    }

    @Test
    fun `an access key consumes key, user and api limits in order`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)

        assertTrue(interceptor.preHandle(request(), response(), invocation))
        assertEquals(listOf("key:11", "user:7", "api:1"), limiter.calls.map { it.label })
        assertEquals(listOf(50, 100, 100), limiter.calls.map { it.limit })
    }

    @Test
    fun `an unlimited user limit is passed as zero`() {
        val principal = apiKeyPrincipal(rateLimit = 40)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 0, key = 40, api = 100)

        assertTrue(interceptor.preHandle(request(), response(), invocation))
        assertEquals(listOf(40, 0, 100), limiter.calls.map { it.limit })
    }

    @Test
    fun `a rejected key limit stops before the api limit`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)
        limiter.rejected += RateLimitDimension.KEY_TOTAL

        val ex =
            assertFailsWith<RequestTooFrequentException> {
                interceptor.preHandle(request(), response(), invocation)
            }

        assertEquals("key", ex.scope)
        assertTrue(ex.rateLimited)
        assertEquals(listOf("key:11"), limiter.calls.map { it.label })
        verify(redisProvider).increment("${TOKEN_ISSUER}_apimetrics_ratelimited:$API_CODE", 1L, 120L)
    }

    @Test
    fun `the response headers expose the tightest limit`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)
        limiter.remaining[RateLimitDimension.USER_TOTAL] = 90
        limiter.remaining[RateLimitDimension.KEY_TOTAL] = 30
        limiter.remaining[RateLimitDimension.API_TOTAL] = 60

        val response = response()
        assertTrue(interceptor.preHandle(request(), response, invocation))
        assertEquals("50", response.getHeader("X-RateLimit-Limit"))
        assertEquals("30", response.getHeader("X-RateLimit-Remaining"))
    }

    @Test
    fun `an anonymous call consumes the ip and api limits`() {
        api.needKey = 0
        Mockito.`when`(rateLimitPolicy.anonymousIpTotal()).thenReturn(5)
        Mockito.`when`(rateLimitPolicy.apiTotal(api, plugin)).thenReturn(100)

        val request = request()
        assertTrue(interceptor.preHandle(request, response(), invocation))

        assertEquals(listOf("ip:$CLIENT_IP", "api:1"), limiter.calls.map { it.label })
        assertEquals(BigDecimal.ZERO, request.getAttribute("apimanagement.cost"))
        assertEquals(ApiInterface.BillingMode.FREE, request.getAttribute("apimanagement.billingMode"))
        assertNull(request.getAttribute("apimanagement.userId"))
    }

    @Test
    fun `a login user consumes user and api limits only`() {
        authenticate(loginUser(userId = 5))
        Mockito.`when`(rateLimitPolicy.userTotal()).thenReturn(100)
        Mockito.`when`(rateLimitPolicy.apiTotal(api, plugin)).thenReturn(100)

        val request = request()
        assertTrue(interceptor.preHandle(request, response(), invocation))

        assertEquals(listOf("user:5", "api:1"), limiter.calls.map { it.label })
        assertEquals(5L, request.getAttribute("apimanagement.userId"))
    }

    @Test
    fun `a login user also consumes the shared user quota`() {
        authenticate(loginUser(userId = 7))
        Mockito.`when`(rateLimitPolicy.userTotal()).thenReturn(100)
        Mockito.`when`(rateLimitPolicy.apiTotal(api, plugin)).thenReturn(100)
        Mockito.`when`(rateLimitPolicy.userQuota()).thenReturn(5L)
        Mockito.`when`(rateLimitPolicy.userQuotaPeriod()).thenReturn(LONG_PERIOD)
        Mockito.`when`(redisProvider.increment(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(6L)

        assertFailsWith<QuotaExceededException> { interceptor.preHandle(request(), response(), invocation) }
        verify(redisProvider).increment("${TOKEN_ISSUER}_user_quota:7:0", 1L, LONG_PERIOD.toLong())
    }

    @Test
    fun `a user quota rejection does not touch the key quota or the rate limit metric`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)
        Mockito.`when`(rateLimitPolicy.userQuota()).thenReturn(5L)
        Mockito.`when`(rateLimitPolicy.userQuotaPeriod()).thenReturn(LONG_PERIOD)
        Mockito.`when`(rateLimitPolicy.keyQuota(principal)).thenReturn(0L)
        Mockito.`when`(redisProvider.increment(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(6L)

        assertFailsWith<QuotaExceededException> { interceptor.preHandle(request(), response(), invocation) }

        verify(redisProvider).increment("${TOKEN_ISSUER}_user_quota:7:0", 1L, LONG_PERIOD.toLong())
        verify(redisProvider, times(1)).increment(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong())
        verify(redisProvider, never())
            .increment("${TOKEN_ISSUER}_apimetrics_ratelimited:$API_CODE", 1L, 120L)
    }

    @Test
    fun `a key quota rejection stops the request`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)
        Mockito.`when`(rateLimitPolicy.userQuota()).thenReturn(0L)
        Mockito.`when`(rateLimitPolicy.keyQuota(principal)).thenReturn(2L)
        Mockito.`when`(rateLimitPolicy.keyQuotaPeriod(principal)).thenReturn(LONG_PERIOD)
        Mockito.`when`(redisProvider.increment(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(3L)

        assertFailsWith<QuotaExceededException> { interceptor.preHandle(request(), response(), invocation) }

        verify(redisProvider).increment("${TOKEN_ISSUER}_key_quota:11:0", 1L, LONG_PERIOD.toLong())
    }

    @Test
    fun `zero quotas do not touch Redis`() {
        val principal = apiKeyPrincipal(rateLimit = 50)
        authenticate(principal)
        stubAccessKeyLimits(principal, user = 100, key = 50, api = 100)
        Mockito.`when`(rateLimitPolicy.userQuota()).thenReturn(0L)
        Mockito.`when`(rateLimitPolicy.keyQuota(principal)).thenReturn(0L)

        assertTrue(interceptor.preHandle(request(), response(), invocation))
        verify(redisProvider, never()).increment(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong())
    }

    private fun stubAccessKeyLimits(principal: ApiKeyPrincipal, user: Int, key: Int, api: Int) {
        Mockito.`when`(rateLimitPolicy.userTotal()).thenReturn(user)
        Mockito.`when`(rateLimitPolicy.keyTotal(principal)).thenReturn(key)
        Mockito.`when`(rateLimitPolicy.apiTotal(this.api, plugin)).thenReturn(api)
    }

    private fun apiKeyPrincipal(rateLimit: Int) = ApiKeyPrincipal().apply {
        keyId = 11L
        userId = 7L
        permissions = listOf(API_CODE)
        this.rateLimit = rateLimit
        quota = 0
        quotaPeriod = 0
    }

    private fun loginUser(userId: Long) = LoginUser(
        User().apply {
            id = userId
            enable = 1
            locking = 0
        },
    )

    private fun authenticate(principal: Any) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }

    private fun request() = MockHttpServletRequest().apply { remoteAddr = CLIENT_IP }

    private fun response() = MockHttpServletResponse()

    private companion object {
        const val PLUGIN_ID = "test"
        const val API_CODE = "api:test:v1:ping"
        const val TOKEN_ISSUER = "ApiManagement"
        const val LONG_PERIOD = Int.MAX_VALUE
        const val CLIENT_IP = "203.0.113.1"
    }

    private class RecordingRateLimiter : RateLimiter {
        data class Call(val dimension: RateLimitDimension, val scopeId: String, val limit: Int) {
            val label get() = "${dimension.segment}:$scopeId"
        }

        val calls = mutableListOf<Call>()
        val rejected = mutableSetOf<RateLimitDimension>()
        val remaining = mutableMapOf<RateLimitDimension, Int>()

        override fun acquire(dimension: RateLimitDimension, scopeId: String, limit: Int): RateLimitResult {
            calls += Call(dimension, scopeId, limit)
            val allowed = dimension !in rejected
            return RateLimitResult(
                dimension = dimension,
                allowed = allowed,
                limit = limit,
                remaining = if (allowed) remaining[dimension] ?: (limit - 1).coerceAtLeast(0) else 0,
                retryAfterSeconds = 60,
            )
        }
    }
}
