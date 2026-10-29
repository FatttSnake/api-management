package top.fatweb.apimanagement.api.v1

/**
 * Route registry available to a starting plugin.
 *
 * Routes are relative to `/api/{pluginId}/v{apiVersion}`. Registration is accepted only while
 * [ApiPlugin.onStart] is running; later calls fail.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ApiRoutes {
    /**
     * Registers a route.
     *
     * @param route route definition
     * @param handler request handler
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun register(route: RouteDefinition, handler: ApiHandler)

    /**
     * Registers a route.
     *
     * @param method HTTP method
     * @param path path relative to the plugin namespace
     * @param apiVersion declared API version, at least 1
     * @param operation operation metadata
     * @param handler request handler
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun register(method: String, path: String, apiVersion: Int, operation: OperationMetadata, handler: ApiHandler) {
        register(RouteDefinition(method, path, apiVersion, operation), handler)
    }
}
