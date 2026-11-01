package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.ApiPlugin
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginModuleGraph
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute

/**
 * A plugin whose `onStart` has completed.
 *
 * The caller owns the lifecycle: call [purge] when the plugin's data is being deleted, then
 * [stop], then [uninstall] on a real uninstall, and finally [close] to release the class loaders.
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
    val context: GatewayContextImpl,
) : AutoCloseable {
    /**
     * Runs the config-changed hook with the plugin class loader as the thread context loader.
     *
     * @param changedKeys Declared keys whose effective value changed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun configChanged(changedKeys: Set<String>) {
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = classLoader
        try {
            plugin.onConfigChanged(context, changedKeys)
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
    }

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
     * Releases the plugin capabilities, class loader and module references.
     *
     * Capabilities are closed first: a datasource pool may still need classes from the plugin
     * class loader while it shuts its connections down.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close() {
        context.close()
        classLoader.close()
        moduleGraph.close()
    }
}
