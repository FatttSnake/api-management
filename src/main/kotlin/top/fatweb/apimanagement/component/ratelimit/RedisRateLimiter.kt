package top.fatweb.apimanagement.component.ratelimit

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties

/**
 * Redis-backed sliding window rate limiter.
 *
 * Uses the Redis server clock and a single-key Lua script, so counters stay consistent across
 * gateway instances. Redis failures fail open: a rate limiter outage must not take the gateway
 * down with it.
 *
 * @param stringRedisTemplate string template used for the Lua invocation
 * @param serverProperties server properties
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
@Component
class RedisRateLimiter(
    private val stringRedisTemplate: StringRedisTemplate,
    private val serverProperties: ServerProperties,
) : RateLimiter {
    private val logger = LoggerFactory.getLogger(RedisRateLimiter::class.java)
    private val script =
        DefaultRedisScript(
            ClassPathResource("lua/sliding_window.lua").inputStream.bufferedReader().use { it.readText() },
            String::class.java,
        )

    override fun acquire(dimension: RateLimitDimension, scopeId: String, limit: Int): RateLimitResult {
        if (limit <= 0) {
            return RateLimitResult(
                dimension = dimension,
                allowed = true,
                limit = 0,
                remaining = 0,
                retryAfterSeconds = 0,
            )
        }

        val key = "${serverProperties.security.tokenIssuer}_rl:{${dimension.segment}:$scopeId}"
        return try {
            val raw =
                stringRedisTemplate.execute(
                    script,
                    listOf(key),
                    limit.toString(),
                    WINDOW_SECONDS.toString(),
                    TTL_SECONDS.toString(),
                ) ?: return failOpen(dimension, limit)
            parse(dimension, limit, raw)
        } catch (ex: Exception) {
            logger.warn("Rate limiter unavailable for {}:{}; allowing the request", dimension.segment, scopeId, ex)
            failOpen(dimension, limit)
        }
    }

    /**
     * Parse the Lua response
     *
     * @param dimension counted dimension
     * @param limit configured limit
     * @param raw comma-separated script output
     * @return acquisition outcome
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    internal fun parse(dimension: RateLimitDimension, limit: Int, raw: String): RateLimitResult {
        val parts = raw.split(',')
        require(parts.size == 4) { "Unexpected rate limiter response: $raw" }
        return RateLimitResult(
            dimension = dimension,
            allowed = parts[0] == "1",
            limit = limit,
            remaining = parts[3].toInt().coerceAtLeast(0),
            retryAfterSeconds = parts[2].toLong().coerceAtLeast(0),
        )
    }

    private fun failOpen(dimension: RateLimitDimension, limit: Int) = RateLimitResult(
        dimension = dimension,
        allowed = true,
        limit = limit,
        remaining = limit,
        retryAfterSeconds = 0,
    )

    private companion object {
        const val WINDOW_SECONDS = 60L
        const val TTL_SECONDS = 125L
    }
}
