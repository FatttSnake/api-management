package top.fatweb.apimanagement.exception

/**
 * Plugin datasource exception
 *
 * Raised when the datasource of a plugin cannot be configured: the plugin does not
 * declare one, its stored password cannot be decrypted, or the connection it describes
 * cannot be established.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class PluginDatasourceException(message: String) : RuntimeException(message)
