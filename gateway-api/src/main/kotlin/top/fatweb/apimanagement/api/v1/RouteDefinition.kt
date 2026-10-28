package top.fatweb.apimanagement.api.v1

import java.util.Locale

/**
 * A route contributed by a plugin.
 *
 * The path is relative to `/api/{pluginId}/v{apiVersion}`. A request may select a higher
 * version than the one declared here; the gateway picks the highest declared version not above
 * the requested one.
 *
 * @property method HTTP method
 * @property path path relative to the plugin and API version
 * @property apiVersion declared API version, at least 1
 * @property operation operation metadata
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class RouteDefinition(method: String, path: String, val apiVersion: Int, val operation: OperationMetadata) {
    /**
     * HTTP method, upper-cased.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val method: String = method.trim().uppercase(Locale.ROOT)

    /**
     * Normalized path.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val path: String = normalizePath(path)

    init {
        require(this.method.isNotBlank()) { "method must not be blank" }
        require(apiVersion >= 1) { "apiVersion must be at least 1" }
    }

    private companion object {
        fun normalizePath(value: String): String {
            var normalized = value.trim()
            require(!normalized.contains('?') && !normalized.contains('#')) {
                "path must not contain a query or fragment"
            }
            if (normalized.isNotEmpty() && !normalized.startsWith('/')) {
                normalized = "/$normalized"
            }
            if (normalized.length > 1 && normalized.endsWith('/')) {
                normalized = normalized.dropLast(1)
            }
            return normalized
        }
    }
}
