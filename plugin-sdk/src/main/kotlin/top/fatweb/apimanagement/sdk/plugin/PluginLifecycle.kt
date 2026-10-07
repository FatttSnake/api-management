package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional hooks invoked by the gateway around a mount and an unmount. All methods have
 * empty default implementations so a plugin only overrides what it needs.
 *
 * A mount is not a one-off event. The gateway mounts a plugin when it is installed, when the
 * gateway starts and re-hydrates it, on `reload`, and whenever a save touches a value one of
 * its datasources is composed from - and every mount runs [onStart]. A remount tears the plugin
 * down first, so a plugin has to expect its own [onStop] in the middle of its lifetime and must
 * not keep anything it needs in memory alone.
 *
 * The four hooks each fire at one distinct moment:
 * - [onStart] on every mount, before the plugin serves anything
 * - [onStop] on every unmount, after the last request it was serving has finished
 * - [onUninstall] after the plugin has been taken out of the gateway, on an uninstall and on
 *   an upgrade
 * - [onPurge] only when an uninstall asked for the plugin's data to be deleted, and never on
 *   an upgrade
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginLifecycle {
    /**
     * Called on every mount, once the plugin's context and datasources are ready and before
     * any of its routes are registered
     *
     * Runs on the first install, on a gateway restart, on a `reload`, and after a save that
     * changed one of the plugin's datasource values - so it is where idempotent setup belongs,
     * such as `CREATE TABLE IF NOT EXISTS` or seeding a row that is looked up by name. A mount
     * that replaces a running one builds the new instance before the old one is taken down, so
     * this may run while an earlier instance is still serving.
     *
     * Nothing the plugin itself serves is reachable yet when this runs, and [PluginContext]
     * cannot describe it yet either: `getInterfaceInfo` reports the previous mount's interface
     * on a remount and nothing at all on a first install.
     *
     * **Throwing here does not fail an install.** This is the hook a plugin reaches its
     * datasources from, so it is where a connection the administrator has not got right yet
     * fails - refusing the install over that would take away the configuration form needed to
     * fix it. The gateway instead keeps the plugin installed, serves nothing on its behalf, and
     * records the reason where the administrator can read it, so that correcting the
     * configuration mounts the plugin properly.
     *
     * The same applies to a `reload` and to a save that changed one of the plugin's datasource
     * values: the plugin is taken down rather than left running on the configuration it came up
     * with, and the reason lands in the same place. A plugin that cannot start on what it was
     * just given has to be seen not to have started.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onStart(context: PluginContext) {}

    /**
     * Called before the plugin is unmounted, for every unmount
     *
     * Fires before a remount replaces the plugin (`reload`, an upgrade, a
     * datasource-changing save), before an uninstall, and at application shutdown. Release
     * what the plugin holds itself here - a datasource the gateway injected and the class
     * loader are closed after this returns, and no request is still running by the time it is
     * called.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onStop(context: PluginContext) {}

    /**
     * Called after the plugin has been taken out of the gateway
     *
     * Runs after the plugin's row, routes and permission tree are gone, and after
     * [onStop]. Note that an **upgrade uninstalls the old version before installing the
     * new one**, so this runs on an upgrade too - do not treat it as "the plugin is never
     * coming back" and destroy data here. It is also too late to reach the plugin's
     * datasources, which were closed with the rest of the mount: use [onPurge] for anything
     * that has to delete data, and this for flushing what the plugin still can reach.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onUninstall(context: PluginContext) {}

    /**
     * Called when the plugin is uninstalled with its data asked to be deleted
     *
     * Runs only for an uninstall that asked for the data to be purged, and only when it is
     * really an uninstall: an upgrade, a `reload`, a remount and application shutdown never
     * trigger it, so a plugin may delete freely here.
     *
     * It runs while the plugin's datasources, storage and settings are all still available and
     * no request is in flight, which is what makes it the place to drop the plugin's own
     * tables - especially in an external MySQL the gateway will not touch - to release the
     * content-addressed keys it saved through `saveContent`, and to remove anything it holds
     * outside the gateway. The gateway deletes its own records (settings, the SQLite
     * databases it created, the plugin's files) after this returns.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onPurge(context: PluginContext) {}
}
