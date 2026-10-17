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
     * an upgrade too - it says this version is going away, never that the plugin is. What decides
     * it is whether the declared version changed: an upgrade changes it and this runs, while a
     * `reload`, a remount driven by a configuration change, and a shutdown all put the same jar
     * back up and do not, and neither does replacing a version with another build of itself.
     *
     * It is not a place to delete anything. Its routes are gone and its requests have finished,
     * and its datasources are closed by then, which is what keeps it from being an obvious place
     * to - but the plugin's storage and its settings are both still reachable from here, and a
     * plugin that released those would lose them on every release rather than only when an
     * administrator asked for them to go. Deleting data belongs to `onPurge`, which runs nowhere
     * near an upgrade. Its class loader is still open, so its own classes still load.
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
     * plugin's datasources, storage and settings are all still available. A plugin that is not
     * mounted is mounted first so that it can run this, with its `onStart` deliberately not run
     * with it - so this may be the only hook a plugin sees on the way out.
     *
     * Throwing here refuses the uninstall. That is the one hook where a failure is not merely
     * logged, because the alternative is worse than a failed uninstall: the administrator's
     * record of the plugin would be deleted while the data this hook exists to drop - some of it
     * in a database the gateway never touches - stays behind with nothing left that could ever
     * remove it. Nothing is deleted before this returns, so a failure leaves an uninstall that
     * can simply be run again.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onPurge(context: PluginContext) {}
}
