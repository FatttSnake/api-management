package top.fatweb.apimanagement.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.stereotype.Component

/**
 * Exception filter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class ExceptionFilter : Filter {
    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain?,
    ) {
        try {
            filterChain!!.doFilter(servletRequest, servletResponse)
        } catch (e: Exception) {
            servletRequest?.setAttribute("filter.error", unwrap(e))
            servletRequest?.getRequestDispatcher("/error/thrown")?.forward(servletRequest, servletResponse)
        }
    }

    // Spring wraps a handler failure in ServletException("Request processing failed: ..."), and
    // that wrapper matches no @ExceptionHandler branch. Peel it off so the advice sees the cause.
    private fun unwrap(ex: Exception): Exception {
        var current = ex
        while (current is ServletException && current.cause is Exception) {
            current = current.cause as Exception
        }
        return current
    }
}
