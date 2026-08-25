package top.fatweb.apimanagement.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * Jwt access denied handler
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see AccessDeniedHandler
 */
@Component
class JwtAccessDeniedHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        throw accessDeniedException
    }
}
