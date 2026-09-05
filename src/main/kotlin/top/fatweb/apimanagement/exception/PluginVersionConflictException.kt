package top.fatweb.apimanagement.exception

/**
 * Plugin version conflict exception
 *
 * Thrown when installing a plugin whose plugin ID is already installed and the new
 * version code is not strictly greater than the currently installed one.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class PluginVersionConflictException(message: String) : RuntimeException(message)
