package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional hooks invoked around a mount and an unmount; all default to no-ops. A plugin is
 * remounted on `reload`, on a save that touches one of its datasource values, and on any save
 * while it is down from a failed mount - so it cannot keep what it needs across the gap in
 * memory alone. A remount builds the new instance and runs its `onStart` while the instance
 * being replaced is still serving, and takes that one down only afterwards, so `onStart` may
 * run alongside a live older instance of the same plugin.
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
     * nothing on its behalf, and records the reason where the administrator can read it. On a
     * remount the instance being replaced has already been taken down by the time this throws,
     * so the plugin stays down until a mount succeeds. That instance is still serving while this
     * runs, and its `onStop` runs after this returns.
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
