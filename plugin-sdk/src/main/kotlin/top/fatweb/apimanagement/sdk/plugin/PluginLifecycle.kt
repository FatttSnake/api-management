package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional hooks invoked around a mount and an unmount; all default to no-ops. A plugin is
 * remounted - and unmounted first - on `reload` and on a save that touches one of its
 * datasource values, so it cannot keep what it needs across the gap in memory alone.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginLifecycle {
    /**
     * Called on every mount, once the plugin's context and datasources are ready and before
     * any of its routes are registered
     *
     * Throwing here does not fail an install: the gateway keeps the plugin installed, serves
     * nothing on its behalf, and records the reason where the administrator can read it.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStart(context: PluginContext) {}

    /**
     * Called before the plugin is unmounted, for every unmount
     *
     * No request is still running by the time it is called, and the injected datasource and
     * class loader are closed after it returns.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStop(context: PluginContext) {}

    /**
     * Called after the plugin has been taken out of the gateway
     *
     * An upgrade uninstalls the old version before installing the new one, so this runs on an
     * upgrade too, and the plugin's datasources are already closed by then.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onUninstall(context: PluginContext) {}

    /**
     * Called when the plugin is uninstalled with its data asked to be deleted
     *
     * An upgrade, a `reload`, a remount or a shutdown never triggers it, and it runs while the
     * plugin's datasources, storage and settings are all still available.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onPurge(context: PluginContext) {}
}
