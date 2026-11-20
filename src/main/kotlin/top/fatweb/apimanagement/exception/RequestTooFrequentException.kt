package top.fatweb.apimanagement.exception

/**
 * Request too frequent exception
 *
 * Carries the rate limit details when the rejection came from a limiter, so the response can
 * use HTTP 429 and Retry-After. The no-argument form is still used by login throttling, which
 * stays on the existing business envelope.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class RequestTooFrequentException(
    message: String = "Request too frequent",
    val scope: String? = null,
    val limit: Int? = null,
    val remaining: Int? = null,
    val retryAfterSeconds: Long? = null,
) : RuntimeException(message) {
    /**
     * Whether this rejection came from a rate limiter
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    val rateLimited: Boolean
        get() = limit != null && retryAfterSeconds != null
}
