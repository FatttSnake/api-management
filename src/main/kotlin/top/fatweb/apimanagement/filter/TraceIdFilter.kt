package top.fatweb.apimanagement.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import top.fatweb.apimanagement.util.TraceIdUtil

/**
 * Trace ID filter
 *
 * Manages Trace ID in MDC for every request. Reads from [TraceIdUtil.TRACE_ID_HEADER]
 * header for distributed tracing support, generates a new one if absent.
 * Propagates the Trace ID back via the response header.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see OncePerRequestFilter
 */
@Component
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val traceId = request.getHeader(TraceIdUtil.TRACE_ID_HEADER)
                ?.takeIf { it.isNotBlank() }
                ?: TraceIdUtil.generate()

            TraceIdUtil.set(traceId)
            response.setHeader(TraceIdUtil.TRACE_ID_HEADER, traceId)

            filterChain.doFilter(request, response)
        } finally {
            TraceIdUtil.clear()
        }
    }
}
