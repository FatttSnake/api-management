package top.fatweb.apimanagement.plugin.runtime.route

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.web.servlet.handler.AbstractHandlerMapping
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Spring MVC handler mapping for plugin routes.
 *
 * The mapping only selects the route; [PluginApiHandlerAdapter] invokes the plugin. This keeps
 * plugin requests independent of Spring's controller annotations and of any Spring version a
 * plugin may bundle internally.
 *
 * @param registry route registry
 * @param interceptors interceptors applied to plugin requests
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginApiHandlerMapping(
    private val registry: PluginRouteRegistry,
    interceptors: List<HandlerInterceptor> = emptyList()
) : AbstractHandlerMapping() {
    init {
        order = HIGHEST_PRECEDENCE + 10
        setInterceptors(*interceptors.toTypedArray())
    }

    /**
     * Selects a plugin route.
     *
     * @param request HTTP request
     * @return invocation, or null when no plugin route matches
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getHandlerInternal(request: HttpServletRequest): Any? {
        val path = request.requestURI.substring(request.contextPath.length)
        val match = registry.match(request.method, path) ?: return null
        return PluginInvocation(match, request.method)
    }
}
