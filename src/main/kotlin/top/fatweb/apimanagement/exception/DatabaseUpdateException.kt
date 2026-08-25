package top.fatweb.apimanagement.exception

/**
 * Database update exception
 *
 * @param message Exception message
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class DatabaseUpdateException(message: String = "Database update failed") : RuntimeException(message)
