package top.fatweb.apimanagement.component.storage

import java.time.Duration

/**
 * File storage provider
 *
 * Two addressing modes are served by the same backend, and they differ in more than
 * their key:
 *
 * - **Content addressing** ([save] / [load] / [exists] / [delete] / [size]) keys an
 *   object by the SHA-256 of its content, so identical content is stored once. These
 *   objects are compressed at rest and can only be read back through [load].
 * - **Location addressing** ([saveAt] / [loadAt] / [existsAt] / [deleteAt] /
 *   [deleteAtPrefix] / [externalUrlAt]) keys an object by a caller-supplied path,
 *   relative to the system-wide location-addressed root, and stores it verbatim.
 *   That is what makes [externalUrlAt] possible.
 *
 * The location family never sees a plugin ID: it receives an already composed key
 * and prefixes only its own physical root, so a backend does not need to know what
 * a plugin is, and the same methods serve any future location-addressed namespace.
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
     * @see ByteArray
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
     * @see ByteArray
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
     * The bytes are stored verbatim, with no compression, so they can later be
     * served through [externalUrlAt].
     *
     * @param key Location key, validated by [StorageKeyUtil.validateLocationKey]
     * @param content File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     */
    fun saveAt(key: String, content: ByteArray)

    /**
     * Load raw bytes from a location-addressed key
     *
     * @param key Location key
     * @return File content, or null when nothing is stored under [key]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
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
     * The two parameters are not the same string, and each backend uses only one of
     * them. A backend that serves the object itself (S3) needs the [key] to sign
     * against; a backend that hands the download back to the gateway (local) needs
     * the [reference] to route on, and signs that instead. Keeping both explicit
     * beats deriving one from the other, which would force the backend to parse a
     * namespace it has no business knowing about.
     *
     * @param key Location key of the object
     * @param reference Public reference to place in a gateway-built URL
     * @param ttl Validity period, already clamped by the caller
     * @return External URL, or null when the backend cannot build an absolute URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Duration
     */
    fun externalUrlAt(key: String, reference: String, ttl: Duration): String?
}
