package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin lifecycle
 *
 * Optional lifecycle hooks invoked by the gateway around a mount and an unmount. All
 * methods have empty default implementations so a plugin only overrides what it needs.
 * Typically used for DDL / data seeding against [PluginContext.datasources].
 *
 * A mount is not a one-off event, so these hooks are not either. The gateway mounts a
 * plugin when it is installed, when the gateway starts and re-hydrates it, on `reload`,
 * and whenever a save touches a value one of its datasources is composed from - and every
 * mount runs [onInstall] and then [onStart]. A remount tears the plugin down first, so a
 * plugin has to expect its own [onStop] in the middle of its lifetime and must not keep
 * anything it needs in memory alone.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginLifecycle {
    /**
     * Called after the plugin is mounted and its routes are registered
     *
     * Runs on every mount, not only the first one - use it for idempotent work such as
     * `CREATE TABLE IF NOT EXISTS`, since it also runs after a `reload` and after a save
     * that changed one of the plugin's datasource values.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onInstall(context: PluginContext) {}

    /**
     * Called immediately after [onInstall] of every mount
     *
     * The last hook of a mount, whether that mount is the first one, a gateway restart,
     * a `reload` or a datasource-changing save. Anything held only in memory is gone by
     * the time it runs again.
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
     * datasource-changing save), before an uninstall, and at application shutdown.
     * Release what the plugin holds itself here - a datasource the gateway injected and
     * the class loader are closed after this returns.
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
     * coming back" and destroy data here. The gateway purges the plugin's settings, its
     * SQLite databases and its files only when an uninstall asks for it, and an upgrade
     * never does.
     *
     * @param context Plugin context
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    fun onUninstall(context: PluginContext) {}
}
