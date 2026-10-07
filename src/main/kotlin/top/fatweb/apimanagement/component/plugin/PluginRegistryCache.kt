package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin

/**
 * Plugin registry cache
 *
 * Holds the API interfaces and the plugin rows the request path reads on every call: the
 * interceptor resolves an incoming path to an interface and then to its plugin before it
 * admits the request.
 *
 * Both are published as one immutable snapshot swapped in a single write, so a reader holds
 * either the set before a refresh or the set after it and never a mixture. That matters more
 * here than for a cache that is only ever read for its own sake: a reader that arrived while
 * an emptied map was being refilled would find no interface where one belongs, and the
 * interceptor answers a miss with "this API is disabled" - taking every plugin's traffic down
 * for as long as the refresh reads the database, whenever any one plugin is edited.
 *
 * The cache is filled by the plugin service as a side effect of its own cache refresh rather
 * than read from it lazily, which keeps the dependency one-way: the service knows about this
 * cache, this cache knows nothing about the service.
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
     * Swapped whole rather than updated in place, so the pair a reader resolves an interface
     * and then its plugin through is always one that was built together.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     * @see ApiPlugin
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
     * @see ApiInterface
     */
    fun getByCode(code: String): ApiInterface? = snapshot.interfaces[code]

    /**
     * Get an installed plugin by plugin ID
     *
     * @param pluginId Plugin ID
     * @return Plugin, or null when no plugin with that ID is installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPlugin
     */
    fun getByPluginId(pluginId: String): ApiPlugin? = snapshot.plugins[pluginId]

    /**
     * Replace both maps at once
     *
     * @param interfaces Interfaces by API code
     * @param plugins Plugins by plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     * @see ApiPlugin
     */
    fun replaceAll(interfaces: Map<String, ApiInterface>, plugins: Map<String, ApiPlugin>) {
        snapshot = Snapshot(interfaces, plugins)
    }
}
