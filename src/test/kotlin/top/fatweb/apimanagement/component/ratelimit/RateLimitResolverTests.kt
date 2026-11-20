package top.fatweb.apimanagement.component.ratelimit

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Rate limit resolver tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 * @see RateLimitResolver
 */
class RateLimitResolverTests {
    @Test
    fun `top-level null and zero mean unlimited`() {
        assertEquals(0, RateLimitResolver.resolveTopLevel(null as Int?))
        assertEquals(0, RateLimitResolver.resolveTopLevel(0))
        assertEquals(60, RateLimitResolver.resolveTopLevel(60))

        assertEquals(0L, RateLimitResolver.resolveTopLevel(null as Long?))
        assertEquals(0L, RateLimitResolver.resolveTopLevel(0L))
        assertEquals(60L, RateLimitResolver.resolveTopLevel(60L))
    }

    @Test
    fun `a child limit inherits the parent and never exceeds it`() {
        assertEquals(0, RateLimitResolver.resolveChildWithParent(null, null))
        assertEquals(0, RateLimitResolver.resolveChildWithParent(0, 0))
        assertEquals(60, RateLimitResolver.resolveChildWithParent(null, 60))
        assertEquals(60, RateLimitResolver.resolveChildWithParent(0, 60))
        assertEquals(60, RateLimitResolver.resolveChildWithParent(60, null))
        assertEquals(60, RateLimitResolver.resolveChildWithParent(60, 0))
        assertEquals(30, RateLimitResolver.resolveChildWithParent(30, 60))
        assertEquals(60, RateLimitResolver.resolveChildWithParent(90, 60))
    }

    @Test
    fun `a quota period falls back to the system value`() {
        assertEquals(86400, RateLimitResolver.resolveQuotaPeriod(null, 86400))
        assertEquals(86400, RateLimitResolver.resolveQuotaPeriod(0, 86400))
        assertEquals(3600, RateLimitResolver.resolveQuotaPeriod(3600, 86400))
    }

    @Test
    fun `the auth failure limit keeps its default and can be disabled`() {
        assertEquals(30, RateLimitResolver.resolveAuthFailureLimit(null))
        assertEquals(0, RateLimitResolver.resolveAuthFailureLimit(0))
        assertEquals(15, RateLimitResolver.resolveAuthFailureLimit(15))
    }
}
