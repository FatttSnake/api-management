package top.fatweb.apimanagement.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import top.fatweb.apimanagement.component.security.JwtProvider
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.exception.TokenHasExpiredException
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.getToken

/**
 * Jwt authentication token filter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see JwtProvider
 * @see OncePerRequestFilter
 */
@Component
class JwtAuthenticationTokenFilter(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val tokenWithPrefix = request.getHeader(serverProperties.security.headerKey)

        if (!StringUtils.hasText(tokenWithPrefix) || "/error/thrown" == request.servletPath ||
            !tokenWithPrefix.startsWith(serverProperties.security.tokenPrefix)
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val token = getToken(serverProperties, tokenWithPrefix)
        jwtProvider.parseJwt(token)

        val redisKeyPattern = "${serverProperties.security.tokenIssuer}_access_*:${token}"
        val redisKeys = redisProvider.keys(redisKeyPattern)
        if (redisKeys.isEmpty()) {
            throw TokenHasExpiredException()
        }

        val loginUser = redisProvider.getObject<LoginUser>(redisKeys.first()) ?: throw TokenHasExpiredException()

        val authenticationToken = UsernamePasswordAuthenticationToken(loginUser, null, loginUser.authorities)
        SecurityContextHolder.getContext().authentication = authenticationToken

        filterChain.doFilter(request, response)
    }
}
