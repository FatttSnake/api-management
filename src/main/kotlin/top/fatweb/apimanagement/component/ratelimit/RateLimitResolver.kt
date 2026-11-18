package top.fatweb.apimanagement.component.ratelimit

/**
 * Resolves effective rate limits and quotas from inherited configuration values.
 *
 * A null or 0 child inherits its parent; when both are concrete the smaller wins. Only a
 * top-level value may use 0 to mean "unlimited".
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
object RateLimitResolver {
    /**
     * Fallback for the per-IP authentication failure limit when it is not configured
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    const val DEFAULT_AUTH_FAILURE_LIMIT = 30

    /**
     * Resolve a top-level limit: null or 0 means unlimited
     *
     * @param configured configured value
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun resolveTopLevel(configured: Int?): Int = configured?.takeIf { it > 0 } ?: 0

    /**
     * Resolve a top-level quota: null or 0 means unlimited
     *
     * @param configured configured value
     * @return effective quota, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun resolveTopLevel(configured: Long?): Long = configured?.takeIf { it > 0 } ?: 0L

    /**
     * Resolve a child limit against its parent: null or 0 inherits, concrete values take the
     * smaller one
     *
     * @param child child value
     * @param parent parent value
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun resolveChildWithParent(child: Int?, parent: Int?): Int {
        val childValue = child?.takeIf { it > 0 }
        val parentValue = parent?.takeIf { it > 0 }
        return when {
            childValue == null -> parentValue ?: 0
            parentValue == null -> childValue
            else -> minOf(childValue, parentValue)
        }
    }

    /**
     * Resolve a quota period: a positive key value overrides the system default
     *
     * @param keyPeriod key period in seconds
     * @param systemDefault system period in seconds
     * @return effective period in seconds
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun resolveQuotaPeriod(keyPeriod: Int?, systemDefault: Int): Int = keyPeriod?.takeIf { it > 0 } ?: systemDefault

    /**
     * Resolve the per-IP authentication failure limit: unset keeps the safe default, an
     * explicit 0 disables the guard
     *
     * @param configured configured value
     * @return effective limit, 0 when disabled
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun resolveAuthFailureLimit(configured: Int?): Int = when {
        configured == null -> DEFAULT_AUTH_FAILURE_LIMIT
        configured <= 0 -> 0
        else -> configured
    }
}
