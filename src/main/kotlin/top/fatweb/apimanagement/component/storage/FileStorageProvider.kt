package top.fatweb.apimanagement.component.storage

import java.time.Duration

/**
 * File storage provider
 *
 * Content addressing keys and compresses an object by the SHA-256 of its content and
 * can only read it back through [load]; location addressing stores it verbatim under a
 * caller-supplied path, which is what makes [externalUrlAt] possible.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface FileStorageProvider {
    /**
     * Save file to storage
     *
     * @param content File
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun save(content: ByteArray): String

    /**
     * Save string to storage
     *
     * @param content String
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun save(content: String): String

    /**
     * Load file from storage
     *
     * @param key File SHA-256 key
     * @return File
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun load(key: String): ByteArray?

    /**
     * Check if the file is stored in storage
     *
     * @param key File SHA-256 key
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun exists(key: String): Boolean

    /**
     * Delete file from storage
     *
     * @param key File SHA-256 key
     * @return true=success; false=fail
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(key: String): Boolean

    /**
     * Get file size (compressed) from storage
     *
     * @param key File SHA-256 key
     * @return File size in bytes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun size(key: String): Long?

    /**
     * Save raw bytes at a location-addressed key
     *
     * @param key Location key, validated by [StorageKeyUtil.validateLocationKey]
     * @param content File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveAt(key: String, content: ByteArray)

    /**
     * Load raw bytes from a location-addressed key
     *
     * @param key Location key
     * @return File content, or null when nothing is stored under [key]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadAt(key: String): ByteArray?

    /**
     * Check if a location-addressed key is stored
     *
     * @param key Location key
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsAt(key: String): Boolean

    /**
     * Delete a location-addressed key
     *
     * @param key Location key
     * @return true=deleted; false=nothing was stored under the key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteAt(key: String): Boolean

    /**
     * Delete every object whose key starts with `{key}/`
     *
     * @param key Location key used as a prefix, without a trailing slash
     * @return Number of objects removed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteAtPrefix(key: String): Int

    /**
     * Build a login-free external URL for a location-addressed key
     *
     * Each backend signs only one of the two: [key] when it serves the object itself,
     * [reference] when the download goes back through the gateway.
     *
     * @param key Location key of the object
     * @param reference Public reference to place in a gateway-built URL
     * @param ttl Validity period, already clamped by the caller
     * @return External URL, or null when the backend cannot build an absolute URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun externalUrlAt(key: String, reference: String, ttl: Duration): String?
}
