package top.fatweb.apimanagement.exception

/**
 * Database insert exception
 *
 * @param message Exception message
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class DatabaseInsertException(message: String = "Database insert failed") : RuntimeException(message)
