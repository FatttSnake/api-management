package top.fatweb.apimanagement.exception

/**
 * Plugin purge failed exception
 *
 * Thrown when uninstalling a plugin with its data asked to be deleted and the plugin's own
 * `onPurge` hook failed, or the plugin could not be mounted to run it. The uninstall is
 * refused rather than completed: `onPurge` is where a plugin drops the tables it owns, some
 * of them in a database the gateway never touches, so completing the uninstall on top of a
 * failed purge would destroy the administrator's record of the plugin while leaving that
 * plugin's data behind with nothing left to ever remove it.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginPurgeFailedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
