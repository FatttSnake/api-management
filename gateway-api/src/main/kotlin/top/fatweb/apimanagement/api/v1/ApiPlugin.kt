package top.fatweb.apimanagement.api.v1

/**
 * Plugin entry point.
 *
 * The gateway creates the class named by `mainClass` in the plugin descriptor, calls [onStart]
 * to collect routes, and calls the remaining hooks while stopping, uninstalling and purging.
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
