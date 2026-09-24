package top.fatweb.apimanagement.component.storage

/**
 * File storage key util
 *
 * Owns the key formats and every check that keeps a caller-supplied key from
 * escaping the location it was meant for.
 *
 * A content key is exactly one lowercase SHA-256 hex digest. A location key is a
 * relative, '/'-separated path below the system-wide location-addressed root, and
 * is validated generically: the validator knows nothing about who owns a key, so
 * every namespace check lives with the code that composes the key ([pluginKey] for
 * plugins).
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object StorageKeyUtil {
    /**
     * Plugin ID pattern; kept in sync with the installer's own check
     */
    val PLUGIN_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

    /**
     * Content key pattern: one lowercase SHA-256 hex digest
     */
    val CONTENT_KEY_REGEX = Regex("^[0-9a-f]{64}$")

    /**
     * Path segment pattern: URL-safe, no separator, no traversal, no Windows special
     */
    private val SEGMENT_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")

    /**
     * Names Windows reserves as devices whatever the extension
     */
    private val WINDOWS_RESERVED_REGEX = Regex("^(?i:con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?$")

    /**
     * Prefix every plugin file lives under, inside the location-addressed root
     */
    private const val PLUGIN_DATA_DIR = "plugin-data"

    private const val MAX_LOCATION_KEY_LENGTH = 1024
    private const val MAX_SEGMENT_LENGTH = 200

    /**
     * Compose the location key of a plugin file
     *
     * The plugin ID is composed in from the caller's own identity, never taken as
     * an argument, so the result is always confined to that plugin's namespace.
     *
     * @param pluginId Plugin ID
     * @param path Relative path inside the plugin's namespace
     * @return Location key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginKey(pluginId: String, path: String): String {
        require(PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$PLUGIN_DATA_DIR/$pluginId/${validatePath(path)}"
    }

    /**
     * Compose the public reference of a plugin file
     *
     * The reference is what an external download URL carries and what its signature
     * covers. It is deliberately not the location key: the storage layout stays an
     * internal detail, so changing it cannot invalidate links already handed out.
     *
     * @param pluginId Plugin ID
     * @param path Relative path inside the plugin's namespace
     * @return Public reference
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginReference(pluginId: String, path: String): String {
        require(PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$pluginId/${validatePath(path)}"
    }

    /**
     * Compose the location key every file of a plugin lives under
     *
     * Used as the prefix when the plugin's files are purged, so a future sibling
     * namespace under the same plugin is not destroyed along with them.
     *
     * @param pluginId Plugin ID
     * @return Location key prefix
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginBaseKey(pluginId: String): String {
        require(PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$PLUGIN_DATA_DIR/$pluginId"
    }

    /**
     * Validate a location key
     *
     * Generic on purpose: a location key is a relative path below the
     * location-addressed root and nothing more. Who is allowed to write where is
     * decided by whoever composes the key, not by this check.
     *
     * @param key Location key
     * @throws IllegalArgumentException when the key is malformed or could resolve
     *         outside the location-addressed root
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun validateLocationKey(key: String) {
        require(key.isNotEmpty()) { "Location key must not be empty" }
        require(key.length <= MAX_LOCATION_KEY_LENGTH) { "Location key is too long: ${key.length} characters" }
        require(!key.startsWith("/")) { "Location key must be relative: '$key'" }
        require(!key.endsWith("/")) { "Location key must not end with '/': '$key'" }

        key.split("/").forEach(::validateSegment)
    }

    /**
     * Validate a relative path inside a namespace
     *
     * @param path Relative path
     * @return The validated path
     * @throws IllegalArgumentException when the path is malformed or escapes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun validatePath(path: String): String {
        require(path.isNotEmpty()) { "Path must not be empty" }
        require(!path.startsWith("/")) { "Path must be relative: '$path'" }
        require(!path.endsWith("/")) { "Path must not end with '/': '$path'" }

        path.split("/").forEach(::validateSegment)

        return path
    }

    /**
     * Validate one path segment
     *
     * The charset restriction is what makes this safe on Windows as well as Linux:
     * banning the backslash means `..\..\x` cannot be silently reinterpreted as
     * three segments by `Path.resolve`.
     *
     * @param segment Path segment
     * @throws IllegalArgumentException when the segment is malformed or reserved
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun validateSegment(segment: String) {
        require(segment.isNotEmpty()) { "Path must not contain an empty segment" }
        require(segment.length <= MAX_SEGMENT_LENGTH) { "Path segment is too long: ${segment.length} characters" }
        require(segment != "." && segment != "..") { "Path must not contain '.' or '..': '$segment'" }
        require(SEGMENT_REGEX.matches(segment)) {
            "Path segment must match ${SEGMENT_REGEX.pattern} (no '/', '\\', ':', or control characters): '$segment'"
        }
        require(!WINDOWS_RESERVED_REGEX.matches(segment)) { "Path segment is a reserved device name: '$segment'" }
    }
}
