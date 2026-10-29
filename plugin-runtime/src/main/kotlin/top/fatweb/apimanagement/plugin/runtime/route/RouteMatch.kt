package top.fatweb.apimanagement.plugin.runtime.route

/**
 * A route selected for a request.
 *
 * @property route matched route
 * @property requestedVersion API version taken from the URL
 * @property pathParameters extracted path parameters
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class RouteMatch(val route: RegisteredRoute, val requestedVersion: Int, val pathParameters: Map<String, String>)
