package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Plugin config schema cache
 *
 * Holds the parsed schema of every installed plugin. The parsed form is what the
 * plugin context needs on every `getSetting` call, so parsing it per call would put a
 * JSON parse on the hot path of every plugin request.
 *
 * The cache is filled by the plugin service as a side effect of its own cache refresh
 * rather than read from it lazily, which keeps the dependency one-way: the service
 * knows about this cache, this cache knows nothing about the service.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginConfigSchemaCache {
    private val schemas = ConcurrentHashMap<String, PluginConfigSchema>()

    /**
     * Get the parsed config schema of a plugin
     *
     * @param pluginId Plugin ID
     * @return Parsed schema, or null when the plugin declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun get(pluginId: String): PluginConfigSchema? = schemas[pluginId]

    /**
     * Replace the parsed config schema of a plugin
     *
     * @param pluginId Plugin ID
     * @param schema Parsed schema, or null to forget the plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun put(pluginId: String, schema: PluginConfigSchema?) {
        if (schema == null) {
            schemas.remove(pluginId)
        } else {
            schemas[pluginId] = schema
        }
    }

    /**
     * Forget every plugin
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun clear() = schemas.clear()
}
