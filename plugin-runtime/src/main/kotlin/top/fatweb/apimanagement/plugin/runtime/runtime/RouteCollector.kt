package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.ApiHandler
import top.fatweb.apimanagement.api.v1.ApiRoutes
import top.fatweb.apimanagement.api.v1.RouteDefinition
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute

/**
 * Collects routes while a plugin's `onStart` runs.
 *
 * Registration is closed before the plugin is served, so a plugin cannot mutate the route table
 * from a request thread.
 *
 * @param pluginId plugin ID
 * @param classLoader plugin class loader
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal class RouteCollector(private val pluginId: String, private val classLoader: ClassLoader) : ApiRoutes {
    private val routes = mutableListOf<RegisteredRoute>()
    private var open = true

    /**
     * Registers a route.
     *
     * @param route route definition
     * @param handler request handler
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    override fun register(route: RouteDefinition, handler: ApiHandler) {
        check(open) { "Routes can only be registered during onStart" }
        routes += RegisteredRoute(pluginId, route, handler, classLoader)
    }

    /**
     * Closes registration and returns the collected routes.
     *
     * @return routes
     * @throws IllegalArgumentException when two routes share an identity
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    fun freeze(): List<RegisteredRoute> {
        open = false
        val codes = routes.map { it.apiCode }
        require(codes.size == codes.toSet().size) {
            "Duplicate operationId in plugin $pluginId"
        }
        val identities =
            routes.map { route ->
                "${route.definition.method} ${route.definition.apiVersion} ${route.definition.path}"
            }
        require(identities.size == identities.toSet().size) {
            "Duplicate route in plugin $pluginId"
        }
        return routes.toList()
    }

    /**
     * Closes registration without producing routes.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    fun close() {
        open = false
    }
}
