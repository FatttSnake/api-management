package top.fatweb.apimanagement.exception

/**
 * Plugin version conflict exception
 *
 * Thrown when installing a plugin whose plugin ID is already installed at a version code newer
 * than the one being installed. Re-installing the version that is already there is not a
 * conflict: it replaces the installed code in place.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginVersionConflictException(message: String) : RuntimeException(message)
