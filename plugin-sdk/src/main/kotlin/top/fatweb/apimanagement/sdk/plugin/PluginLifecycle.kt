package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional lifecycle hooks invoked by the gateway around install / uninstall and
 * at application startup (re-hydration). All methods have empty default
 * implementations so a plugin only overrides what it needs. Typically used for
 * DDL / data seeding against [PluginContext.datasource].
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginContext
 */
interface PluginLifecycle {
    /**
     * Called after the plugin is installed and its routes are registered
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onInstall(context: PluginContext) {}

    /**
     * Called after the plugin is mounted at application startup
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStart(context: PluginContext) {}

    /**
     * Called before the plugin is unmounted at application shutdown / uninstall
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStop(context: PluginContext) {}

    /**
     * Called after the plugin is uninstalled
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onUninstall(context: PluginContext) {}
}
