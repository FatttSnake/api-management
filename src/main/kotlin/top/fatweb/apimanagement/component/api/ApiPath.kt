package top.fatweb.apimanagement.component.api

import jakarta.servlet.http.HttpServletRequest

/**
 * Parsing of the gateway's API URL shape
 *
 * Every plugin endpoint lives under `/api/{plugin}/v{version}/...`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal object ApiPath {
    /**
     * `/api/{plugin}/v{version}` at the start of the context-path-stripped request URI
     */
    private val REGEX = Regex("/api/([^/]+)/v(\\d+)(/.*)?$")

    /**
     * Plugin a request addresses
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Scoped(
        /**
         * Plugin ID exactly as it appears in the URL
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val plugin: String,
    )

    /**
     * Parse the plugin a request addresses
     *
     * @param request Request being routed or authorized
     * @return Plugin, or null when the URI is not an API URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parse(request: HttpServletRequest): Scoped? {
        val uri = request.requestURI.removePrefix(request.contextPath ?: "")
        val match = REGEX.matchAt(uri, 0) ?: return null
        match.groupValues[2].toIntOrNull() ?: return null
        return Scoped(plugin = match.groupValues[1])
    }
}
