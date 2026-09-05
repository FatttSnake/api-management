package top.fatweb.apimanagement.exception

/**
 * Plugin signature invalid exception
 *
 * Thrown when a plugin jar is missing its signature or fails signature verification
 * (tampered or badly built).
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class PluginSignatureInvalidException : RuntimeException("Plugin signature invalid")
