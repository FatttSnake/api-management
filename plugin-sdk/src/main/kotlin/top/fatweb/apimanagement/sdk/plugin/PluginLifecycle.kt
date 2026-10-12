package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional hooks invoked around a mount and an unmount; all default to no-ops. A plugin is
 * remounted on `reload`, on a save that touches one of its datasource values, and on any save
 * while it is down from a failed mount - so it cannot keep what it needs across the gap in
 * memory alone. A remount builds the new instance and runs its `onStart` while the instance
 * being replaced is still serving, and takes that one down only afterwards, so `onStart` may
 * run alongside a live older instance of the same plugin. A remount driven by a configuration
 * change runs on a background thread, where there is no request to read an identity from and
 * `currentUserId()` is null.
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
     * remount the instance being replaced is still serving while this runs, and its `onStop`
     * runs only after this returns - so this is what takes the plugin down, a moment after the
     * plugin was serving normally.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStart(context: PluginContext) {}

    /**
     * Called before the plugin is unmounted, for every unmount
     *
     * The requests it was serving are waited for first, up to a fixed timeout, after which it is
     * called anyway and the rest of the teardown closes the datasource and the class loader
     * under whatever is still running.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStop(context: PluginContext) {}

    /**
     * Called as the plugin is taken out of the gateway
     *
     * An upgrade takes the old version down on the way to installing the new one, so this runs on
     * an upgrade too - it says this version is going away, never that the plugin is. Its routes are
     * gone and its requests have finished, and its datasources are closed by then, which is what
     * keeps it from being a place to destroy data an upgrade would need back. Its class loader is
     * still open, so its own classes still load.
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
