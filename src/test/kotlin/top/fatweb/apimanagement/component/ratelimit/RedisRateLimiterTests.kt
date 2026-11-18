package top.fatweb.apimanagement.component.ratelimit

import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.data.redis.core.StringRedisTemplate
import top.fatweb.apimanagement.properties.ServerProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Redis rate limiter tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 * @see RedisRateLimiter
 */
class RedisRateLimiterTests {
    private val template = mock(StringRedisTemplate::class.java)
    private val limiter = RedisRateLimiter(template, ServerProperties())

    @Test
    fun `an unlimited dimension never reaches Redis`() {
        val result = limiter.acquire(RateLimitDimension.API_TOTAL, "1", 0)

        assertTrue(result.allowed)
        assertEquals(0, result.limit)
        verifyNoInteractions(template)
    }

    @Test
    fun `the script response is parsed into an allowed result`() {
        val result = limiter.parse(RateLimitDimension.USER_TOTAL, 10, "1,3,12,7")

        assertTrue(result.allowed)
        assertEquals(10, result.limit)
        assertEquals(7, result.remaining)
        assertEquals(12, result.retryAfterSeconds)
    }

    @Test
    fun `the script response is parsed into a rejected result`() {
        val result = limiter.parse(RateLimitDimension.KEY_TOTAL, 5, "0,5,30,0")

        assertFalse(result.allowed)
        assertEquals(5, result.limit)
        assertEquals(0, result.remaining)
        assertEquals(30, result.retryAfterSeconds)
    }
}
