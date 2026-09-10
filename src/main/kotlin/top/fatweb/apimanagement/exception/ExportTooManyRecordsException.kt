package top.fatweb.apimanagement.exception

/**
 * Export too many records exception
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class ExportTooManyRecordsException(limit: Long) : RuntimeException("Export records exceed the limit of $limit")
