package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional hooks invoked around a mount and an unmount; all default to no-ops. A remount runs
 * the new instance's `onStart` while the instance being replaced is still serving, so `onStart`
 * may overlap a live older instance. A remount driven by a configuration change runs on a
 * background thread, where `currentUserId()` is null.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginLifecycle {
    /**
     * Called on every mount, once the context and datasources are ready and before any route
     * is registered
     *
     * Throwing here does not fail an install: the plugin stays installed, serves nothing, and
     * records the reason in `load_error`.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStart(context: PluginContext) {}

    /**
     * Called before every unmount
     *
     * In-flight requests are waited for up to a fixed timeout, after which teardown proceeds
     * regardless.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStop(context: PluginContext) {}

    /**
     * Called as this version is taken out of the gateway
     *
     * An upgrade runs it too: what decides it is whether the declared version changed, not
     * whether the plugin is going away. Datasources are already closed here, but storage and
     * settings remain reachable and the class loader is still open.
     *
     * Never delete data here — this runs on every release. Deletion belongs to `onPurge`.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onUninstall(context: PluginContext) {}

    /**
     * Called when the plugin is uninstalled with its data asked to be deleted
     *
     * An upgrade, a `reload`, a remount and a shutdown never trigger it. It runs while the
     * plugin's datasources, storage and settings are all still available. A plugin that is not
     * mounted is mounted first so it can run this, without `onStart` — so this may be the only
     * hook a plugin sees on the way out.
     *
     * Throwing refuses the uninstall; nothing has been deleted, so it can be retried.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onPurge(context: PluginContext) {}
}
