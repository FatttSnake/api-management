package top.fatweb.apimanagement.plugin.runtime.route

import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser
import top.fatweb.apimanagement.api.v1.ApiHandler
import top.fatweb.apimanagement.api.v1.RouteDefinition

/**
 * A route registered by a running plugin.
 *
 * @property pluginId owning plugin
 * @property definition route definition
 * @property handler request handler
 * @property classLoader plugin class loader used while the handler runs
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class RegisteredRoute(
    val pluginId: String,
    val definition: RouteDefinition,
    val handler: ApiHandler,
    val classLoader: ClassLoader
) {
    /**
     * Stable interface code.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val apiCode: String = "api:$pluginId:v${definition.apiVersion}:${definition.operation.operationId}"

    /**
     * Compiled path pattern.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pattern: PathPattern = PathPatternParser.defaultInstance.parse(
        if (definition.path.isEmpty()) "/" else definition.path
    )

    init {
        require(pluginId.isNotBlank()) { "pluginId must not be blank" }
    }
}
