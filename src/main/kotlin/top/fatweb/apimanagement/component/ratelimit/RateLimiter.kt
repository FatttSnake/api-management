package top.fatweb.apimanagement.component.ratelimit

/**
 * Rate limit dimensions counted per minute
 *
 * @property segment key segment used in the Redis key
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
enum class RateLimitDimension(val segment: String) {
    API_TOTAL("api"),
    USER_TOTAL("user"),
    KEY_TOTAL("key"),
    ANONYMOUS_IP("ip"),
    AUTH_FAILURE("authfail"),
}

/**
 * Outcome of one rate limit acquisition
 *
 * @property dimension counted dimension
 * @property allowed whether the request may proceed
 * @property limit configured limit, 0 when unlimited
 * @property remaining requests left in the current window
 * @property retryAfterSeconds seconds until the limiter can admit a request again
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
data class RateLimitResult(
    val dimension: RateLimitDimension,
    val allowed: Boolean,
    val limit: Int,
    val remaining: Int,
    val retryAfterSeconds: Long,
)

/**
 * Per-minute rate limiter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
interface RateLimiter {
    /**
     * Consume one unit from the dimension, when the limit allows it
     *
     * @param dimension counted dimension
     * @param scopeId dimension identity, e.g. API ID, user ID or IP
     * @param limit configured limit, a value <= 0 means unlimited
     * @return acquisition outcome
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun acquire(dimension: RateLimitDimension, scopeId: String, limit: Int): RateLimitResult
}
