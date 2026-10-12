package top.fatweb.apimanagement.exception

/**
 * Storage link invalid exception
 *
 * Raised for an external storage link whose signature does not verify or whose
 * validity has run out.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class StorageLinkInvalidException : RuntimeException("Storage link is invalid or has expired")
