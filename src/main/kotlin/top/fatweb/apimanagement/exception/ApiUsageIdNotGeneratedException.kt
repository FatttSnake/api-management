package top.fatweb.apimanagement.exception

/**
 * Api usage id not generated exception
 *
 * @param message Exception message
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ApiUsageIdNotGeneratedException(message: String = "API usage ID not generated") : RuntimeException(message)
