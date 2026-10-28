package top.fatweb.apimanagement.api.v1

import java.time.Duration

/**
 * Plugin-scoped storage.
 *
 * Content is addressed by SHA-256, deduplicated and reference-counted. Location-addressed
 * files live in the plugin namespace and support temporary external URLs.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface Storage {
    /**
     * Saves content and returns its SHA-256 key.
     *
     * @param content content bytes
     * @return content key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveContent(content: ByteArray): String

    /**
     * Loads content.
     *
     * @param key content key
     * @return content, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadContent(key: String): ByteArray?

    /**
     * Checks whether content exists.
     *
     * @param key content key
     * @return true when present
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsContent(key: String): Boolean

    /**
     * Releases one reference to content.
     *
     * @param key content key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteContent(key: String)

    /**
     * Writes a file in the plugin namespace.
     *
     * @param path relative path
     * @param content file bytes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveFile(path: String, content: ByteArray)

    /**
     * Reads a file from the plugin namespace.
     *
     * @param path relative path
     * @return file bytes, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadFile(path: String): ByteArray?

    /**
     * Checks whether a file exists.
     *
     * @param path relative path
     * @return true when present
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsFile(path: String): Boolean

    /**
     * Deletes a file.
     *
     * @param path relative path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteFile(path: String)

    /**
     * Builds a temporary external URL for a file using the gateway default TTL.
     *
     * @param path relative path
     * @return URL, or null when the gateway has no public base URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fileExternalUrl(path: String): String? = fileExternalUrl(path, null)

    /**
     * Builds a temporary external URL for a file.
     *
     * @param path relative path
     * @param ttl validity, or null for the gateway default
     * @return URL, or null when the gateway has no public base URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fileExternalUrl(path: String, ttl: Duration?): String?
}
