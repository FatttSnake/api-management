package top.fatweb.apimanagement.exception

/**
 * Plugin not trusted exception
 *
 * Thrown when the plugin's signer public key is not present in the trust store or
 * has been disabled.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class PluginNotTrustedException : RuntimeException("Plugin signer is not trusted")
