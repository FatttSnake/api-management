package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.ApiPlugin
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginModuleGraph
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute

/**
 * A plugin whose `onStart` has completed.
 *
 * The caller owns the lifecycle: call [stop], [purge] and [uninstall] in that order, then
 * [close] to release the class loaders.
 *
 * @property descriptor plugin descriptor
 * @property plugin plugin instance
 * @property classLoader plugin class loader
 * @property moduleGraph acquired module graph
 * @property routes routes collected during `onStart`
 * @property context context handed to the plugin
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class LoadedPlugin internal constructor(
    val descriptor: PluginDescriptor,
    val plugin: ApiPlugin,
    val classLoader: PluginClassLoader,
    val moduleGraph: PluginModuleGraph,
    val routes: List<RegisteredRoute>,
    val context: GatewayContextImpl
) : AutoCloseable {
    /**
     * Stops the plugin.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun stop() {
        plugin.onStop()
    }

    /**
     * Runs the purge hook.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun purge() {
        plugin.onPurge()
    }

    /**
     * Runs the uninstall hook while the class loader is still open.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun uninstall() {
        plugin.onUninstall()
    }

    /**
     * Closes the plugin class loader and releases module references.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close() {
        classLoader.close()
        moduleGraph.close()
    }
}
