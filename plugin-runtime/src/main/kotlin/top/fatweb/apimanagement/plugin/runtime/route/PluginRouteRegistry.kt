package top.fatweb.apimanagement.plugin.runtime.route

import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPattern
import java.util.Locale

/**
 * Gateway-wide route table contributed by mounted plugins.
 *
 * Requests are matched by `/api/{pluginId}/v{n}/...`. A route declared at version `v` also serves
 * requests for a higher version, and the highest declared version wins.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginRouteRegistry {
    private val lock = Any()

    @Volatile
    private var routes: List<RegisteredRoute> = emptyList()

    /**
     * Registers routes for a plugin, replacing any routes it already had.
     *
     * The table is swapped as a whole, so a request either sees the old routes or the new
     * ones and never the gap between them.
     *
     * @param pluginId plugin ID
     * @param newRoutes routes
     * @throws IllegalArgumentException when two routes share an identity
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun replace(pluginId: String, newRoutes: List<RegisteredRoute>) {
        validate(pluginId, newRoutes)
        synchronized(lock) {
            routes = routes.filterNot { it.pluginId == pluginId } + newRoutes
        }
    }

    /**
     * Removes all routes for a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun unregisterPlugin(pluginId: String) {
        synchronized(lock) {
            routes = routes.filterNot { it.pluginId == pluginId }
        }
    }

    /**
     * Returns a snapshot of all routes.
     *
     * @return routes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun all(): List<RegisteredRoute> = routes

    /**
     * Matches a request.
     *
     * @param method HTTP method
     * @param path request path
     * @return match, or null when no route matches
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun match(method: String, path: String): RouteMatch? {
        val parsed = PATH.matchEntire(path) ?: return null
        val pluginId = parsed.groupValues[1]
        val requestedVersion = parsed.groupValues[2].toIntOrNull() ?: return null
        val remaining = parsed.groupValues[3].ifEmpty { "/" }
        val container = PathContainer.parsePath(remaining)
        val normalizedMethod = method.uppercase(Locale.ROOT)

        val snapshot = routes

        val candidates =
            snapshot
                .asSequence()
                .filter { it.pluginId == pluginId }
                .filter { it.definition.method == normalizedMethod }
                .filter { it.definition.apiVersion <= requestedVersion }
                .mapNotNull { route ->
                    route.pattern.matchAndExtract(container)?.let { info ->
                        Candidate(route, info.uriVariables)
                    }
                }.sortedWith(
                    compareByDescending<Candidate> { it.route.definition.apiVersion }
                        .thenComparator { left, right ->
                            PathPattern.SPECIFICITY_COMPARATOR.compare(right.route.pattern, left.route.pattern)
                        },
                ).toList()

        val best = candidates.firstOrNull() ?: return null
        return RouteMatch(best.route, requestedVersion, best.pathParameters)
    }

    private fun validate(pluginId: String, newRoutes: List<RegisteredRoute>) {
        val apiCodes = newRoutes.map { it.apiCode }
        require(apiCodes.size == apiCodes.toSet().size) {
            "Duplicate operationId in plugin $pluginId"
        }
        val identifiers =
            newRoutes.map {
                "${it.definition.method} ${it.definition.apiVersion} ${it.definition.path}"
            }
        require(identifiers.size == identifiers.toSet().size) {
            "Duplicate route in plugin $pluginId"
        }
        newRoutes.forEach { route ->
            require(route.pluginId == pluginId) {
                "Route ${route.apiCode} belongs to ${route.pluginId}, not $pluginId"
            }
        }
    }

    private class Candidate(val route: RegisteredRoute, val pathParameters: Map<String, String>)

    private companion object {
        val PATH = Regex("^/api/([^/]+)/v(\\d+)(/.*)?$")
    }
}
