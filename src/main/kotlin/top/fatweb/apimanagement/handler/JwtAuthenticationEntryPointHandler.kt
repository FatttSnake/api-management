package top.fatweb.apimanagement.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * Jwt authentication entry point handler
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see AuthenticationEntryPoint
 */
@Component
class JwtAuthenticationEntryPointHandler : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        request.setAttribute("filter.error", authException)
        request.getRequestDispatcher("/error/thrown")?.forward(request, response)
    }
}
