package top.fatweb.apimanagement.sdk.plugin

import java.time.Duration

/**
 * Plugin storage
 *
 * Content-addressed objects are compressed and can only be read back through [loadContent];
 * location-addressed files are stored verbatim, which is what makes [fileExternalUrl]
 * possible. Every path is confined to this plugin's namespace, and an escape is rejected.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginContext
 */
interface PluginStorage {
    /**
     * Save content to storage
     *
     * Stored once per distinct byte sequence and reference-counted, so call [deleteContent]
     * exactly once per successful save.
     *
     * @param content File content
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveContent(content: ByteArray): String

    /**
     * Load content from storage
     *
     * @param key File SHA-256 key returned by [saveContent]
     * @return File content, or null when nothing is stored under [key]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadContent(key: String): ByteArray?

    /**
     * Check if the content is stored in storage
     *
     * @param key File SHA-256 key
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsContent(key: String): Boolean

    /**
     * Delete content from storage
     *
     * Only the reference count is decremented - the bytes are retained until the gateway
     * reclaims them - and deleting a key that was never stored is a no-op.
     *
     * @param key File SHA-256 key returned by [saveContent]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteContent(key: String)

    /**
     * Save file to storage
     *
     * Writing to a path that already exists overwrites it, and a path that tries to escape
     * this plugin's namespace is rejected with [IllegalArgumentException].
     *
     * @param path Relative path inside this plugin's namespace
     * @param content File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveFile(path: String, content: ByteArray)

    /**
     * Load file from storage
     *
     * @param path Relative path inside this plugin's namespace
     * @return File content, or null when nothing is stored at [path]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadFile(path: String): ByteArray?

    /**
     * Check if the file is stored in storage
     *
     * @param path Relative path inside this plugin's namespace
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsFile(path: String): Boolean

    /**
     * Delete file from storage
     *
     * Deleting a path that does not exist is a no-op.
     *
     * @param path Relative path inside this plugin's namespace
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteFile(path: String)

    /**
     * Build a login-free external URL for the file stored at [path]
     *
     * It is a bearer credential - valid until [ttl] and not revocable before then - and the
     * file need not exist yet.
     *
     * @param path Relative path inside this plugin's namespace
     * @param ttl How long the URL stays valid, or null for the gateway's configured
     *        default. A value above the gateway's configured maximum is clamped down; a
     *        zero or negative one is rejected, because a URL that never works is never
     *        what a caller means
     * @return External URL, or null when the gateway cannot build an absolute URL
     *         (called outside a request while no public base URL is configured)
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fileExternalUrl(path: String, ttl: Duration? = null): String?
}
