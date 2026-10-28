package top.fatweb.apimanagement.plugin.runtime.route

import top.fatweb.apimanagement.api.v1.RouteDefinition

/**
 * Handler object returned by [PluginApiHandlerMapping].
 *
 * @property match matched route
 * @property requestMethod HTTP method of the request
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginInvocation(
    val match: RouteMatch,
    val requestMethod: String
) {
    /**
     * Matched route.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val route: RegisteredRoute
        get() = match.route

    /**
     * Route definition.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val definition: RouteDefinition
        get() = route.definition

    /**
     * Owning plugin.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: String
        get() = route.pluginId

    /**
     * Stable interface code.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val apiCode: String
        get() = route.apiCode

    /**
     * Plugin class loader.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val classLoader: ClassLoader
        get() = route.classLoader
}
