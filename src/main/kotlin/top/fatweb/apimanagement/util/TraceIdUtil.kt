package top.fatweb.apimanagement.util

import org.slf4j.MDC
import java.util.*

/**
 * Trace ID utility
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object TraceIdUtil {
    const val TRACE_ID_HEADER = "X-Trace-Id"
    private const val MDC_KEY = "traceId"

    /**
     * Generate a unique trace ID
     *
     * @return 32-character hex string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generate(): String = UUID.randomUUID().toString().replace("-", "")

    /**
     * Set trace ID into MDC
     *
     * @param traceId Trace ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun set(traceId: String) = MDC.put(MDC_KEY, traceId)

    /**
     * Get trace ID from MDC
     *
     * @return Trace ID or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(): String? = MDC.get(MDC_KEY)

    /**
     * Clear MDC context
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun clear() = MDC.clear()
}
