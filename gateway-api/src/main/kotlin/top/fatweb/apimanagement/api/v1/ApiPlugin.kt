package top.fatweb.apimanagement.api.v1

/**
 * Plugin entry point.
 *
 * The gateway creates the class named by `mainClass` in the plugin descriptor, calls [onStart]
 * to collect routes, and calls the remaining hooks while config changes, stopping, uninstalling
 * and purging.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ApiPlugin {
    /**
     * Starts the plugin and registers its routes.
     *
     * Registrations are staged and activated only after this method returns normally.
     *
     * @param context gateway capabilities for this plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStart(context: GatewayContext)

    /**
     * Notifies the plugin that an administrator saved its configuration.
     *
     * Called after the save commits, on the mounted instance, with the declared keys whose
     * effective value changed. A save that remounts the plugin - a datasource key changed, or the
     * plugin was down from a load error - does not call this; the new instance receives [onStart]
     * instead.
     *
     * @param context gateway capabilities for this plugin
     * @param changedKeys declared keys whose effective value changed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onConfigChanged(context: GatewayContext, changedKeys: Set<String>) {
    }

    /**
     * Stops serving requests.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onStop() {
    }

    /**
     * Releases external resources before the plugin is removed.
     *
     * Called while the plugin class loader is still open.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onUninstall() {
    }

    /**
     * Deletes plugin-owned data.
     *
     * Called only when an administrator requests a purge, before [onStop].
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun onPurge() {
    }
}
