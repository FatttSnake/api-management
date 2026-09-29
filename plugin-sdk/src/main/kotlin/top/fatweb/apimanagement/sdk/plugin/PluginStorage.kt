package top.fatweb.apimanagement.sdk.plugin

import java.time.Duration

/**
 * Plugin storage
 *
 * The narrow, gateway-sanctioned file channel for a plugin, backed by the
 * gateway's configured storage (`app.storage`). Two addressing modes are offered
 * and they are not interchangeable:
 *
 * - **Content addressing** ([saveContent] / [loadContent] / [existsContent] /
 *   [deleteContent]) stores bytes under their SHA-256 key, so identical content is
 *   stored once and shared between every writer. The gateway compresses these
 *   objects transparently, which is why they can only be read back through
 *   [loadContent] and can never be handed to a browser as a plain URL.
 * - **Location addressing** ([saveFile] / [loadFile] / [existsFile] / [deleteFile]
 *   / [fileExternalUrl]) stores bytes under a path chosen by the plugin. The
 *   gateway stores these bytes verbatim, which is what makes [fileExternalUrl]
 *   possible.
 *
 * A [PluginStorage] instance is bound to a single plugin. Every path is resolved
 * underneath that plugin's own namespace, and no call can address a file belonging
 * to another plugin, to the gateway, or to the content-addressed object store. A
 * path that tries to is rejected rather than silently escaped.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginContext
 */
interface PluginStorage {
    /**
     * Save content to storage
     *
     * Content is stored once per distinct byte sequence and shared between every
     * writer, so storing the same bytes twice reuses the same object and raises the
     * gateway's reference count. Call [deleteContent] exactly once per successful
     * [saveContent] of the same key.
     *
     * @param content File content
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     */
    fun saveContent(content: ByteArray): String

    /**
     * Load content from storage
     *
     * @param key File SHA-256 key returned by [saveContent]
     * @return File content, or null when nothing is stored under [key]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
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
     * Only the gateway's reference count is decremented; the bytes are retained
     * until the gateway reclaims unreferenced objects. Deleting a key that was never
     * stored is a no-op.
     *
     * @param key File SHA-256 key returned by [saveContent]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteContent(key: String)

    /**
     * Save file to storage
     *
     * The bytes are stored verbatim, so the file can later be handed to a browser
     * through [fileExternalUrl]. Writing to a path that already exists overwrites it.
     *
     * [path] is a relative, '/'-separated path of URL-safe segments, e.g.
     * `avatar/123.png`. It must not be empty, must not start or end with '/', must
     * not contain a '.' or '..' segment, a backslash or a colon, and must not try to
     * address another plugin's namespace or the content-addressed object store;
     * such paths are rejected with [IllegalArgumentException].
     *
     * @param path Relative path inside this plugin's namespace
     * @param content File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     */
    fun saveFile(path: String, content: ByteArray)

    /**
     * Load file from storage
     *
     * @param path Relative path inside this plugin's namespace
     * @return File content, or null when nothing is stored at [path]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
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
     * The returned URL can be handed to an unauthenticated caller (a browser, a
     * third-party service) and stays valid for [ttl]. It is a bearer credential:
     * whoever holds it can read the file until it expires, and it cannot be revoked
     * before then.
     *
     * The URL is only a promise — the file need not exist yet, and deleting it later
     * makes the URL fail.
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
     * @see Duration
     */
    fun fileExternalUrl(path: String, ttl: Duration? = null): String?
}
