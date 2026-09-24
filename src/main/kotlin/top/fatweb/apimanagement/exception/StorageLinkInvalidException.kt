package top.fatweb.apimanagement.exception

/**
 * Storage link invalid exception
 *
 * Raised for an external storage link whose signature does not verify or whose
 * validity has run out. The two are deliberately indistinguishable to the caller,
 * so the response cannot be used to probe which paths exist.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class StorageLinkInvalidException : RuntimeException("Storage link is invalid or has expired")
