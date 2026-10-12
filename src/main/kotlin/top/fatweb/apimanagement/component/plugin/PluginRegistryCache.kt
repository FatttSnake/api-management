package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin

/**
 * Plugin registry cache
 *
 * Holds the API interfaces and the plugin rows the request path reads on every call. Both are
 * published as one immutable snapshot swapped in a single write, so a reader never sees a
 * half-refreshed mixture.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiInterface
 * @see ApiPlugin
 * @see PluginConfigSchemaCache
 */
@Component
class PluginRegistryCache {
    /**
     * A consistent pair of the two maps the request path resolves through
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private class Snapshot(
        val interfaces: Map<String, ApiInterface>,
        val plugins: Map<String, ApiPlugin>
    )

    @Volatile
    private var snapshot: Snapshot = Snapshot(emptyMap(), emptyMap())

    /**
     * Get the interface registered under an API code
     *
     * @param code API scoping code
     * @return Interface, or null when no enabled-or-disabled interface carries the code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getByCode(code: String): ApiInterface? = snapshot.interfaces[code]

    /**
     * Get an installed plugin by plugin ID
     *
     * @param pluginId Plugin ID
     * @return Plugin, or null when no plugin with that ID is installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getByPluginId(pluginId: String): ApiPlugin? = snapshot.plugins[pluginId]

    /**
     * Replace both maps at once
     *
     * @param interfaces Interfaces by API code
     * @param plugins Plugins by plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun replaceAll(interfaces: Map<String, ApiInterface>, plugins: Map<String, ApiPlugin>) {
        snapshot = Snapshot(interfaces, plugins)
    }
}
