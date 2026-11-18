package top.fatweb.apimanagement.component.ratelimit

import jakarta.servlet.http.HttpServletResponse

/**
 * Writes rate limit headers onto an API response
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
object RateLimitHeaders {
    const val LIMIT = "X-RateLimit-Limit"
    const val REMAINING = "X-RateLimit-Remaining"
    const val RESET = "X-RateLimit-Reset"
    const val RETRY_AFTER = "Retry-After"

    /**
     * Write the limit, remaining and reset headers
     *
     * @param response servlet response
     * @param result acquisition outcome
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun apply(response: HttpServletResponse, result: RateLimitResult) {
        apply(response, result.limit, result.remaining, result.retryAfterSeconds)
    }

    /**
     * Write the limit, remaining and reset headers
     *
     * @param response servlet response
     * @param limit configured limit
     * @param remaining requests left in the current window
     * @param resetSeconds seconds until the current window ends
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun apply(response: HttpServletResponse, limit: Int, remaining: Int, resetSeconds: Long) {
        response.setHeader(LIMIT, limit.toString())
        response.setHeader(REMAINING, remaining.toString())
        response.setHeader(RESET, resetSeconds.toString())
    }

    /**
     * Write the limit headers plus Retry-After for a rejected request
     *
     * @param response servlet response
     * @param result acquisition outcome
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun applyRetry(response: HttpServletResponse, result: RateLimitResult) {
        applyRetry(response, result.limit, result.remaining, result.retryAfterSeconds, result.retryAfterSeconds)
    }

    /**
     * Write the limit headers plus Retry-After for a rejected request
     *
     * @param response servlet response
     * @param limit configured limit
     * @param remaining requests left in the current window
     * @param resetSeconds seconds until the current window ends
     * @param retryAfterSeconds seconds until the limiter can admit a request again
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun applyRetry(
        response: HttpServletResponse,
        limit: Int,
        remaining: Int,
        resetSeconds: Long,
        retryAfterSeconds: Long,
    ) {
        apply(response, limit, remaining, resetSeconds)
        response.setHeader(RETRY_AFTER, retryAfterSeconds.toString())
    }
}
