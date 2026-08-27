package top.fatweb.apimanagement.exception

/**
 * API quota exceeded exception
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class QuotaExceededException : RuntimeException("API quota exceeded")
