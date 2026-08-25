package top.fatweb.apimanagement.component.storage

/**
 * File storage provider interface
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
}
