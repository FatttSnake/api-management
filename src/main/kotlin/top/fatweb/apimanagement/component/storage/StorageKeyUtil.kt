package top.fatweb.apimanagement.component.storage

import top.fatweb.apimanagement.sdk.plugin.PluginDescriptor

/**
 * File storage key util
 *
 * A content key is one lowercase SHA-256 hex digest; a location key is a relative,
 * '/'-separated path below the location-addressed root, validated generically.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object StorageKeyUtil {
    /**
     * Content key pattern: one lowercase SHA-256 hex digest
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    val CONTENT_KEY_REGEX = Regex("^[0-9a-f]{64}$")

    /**
     * Path segment pattern: URL-safe, no separator, no traversal, no Windows special
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private val SEGMENT_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")

    /**
     * Names Windows reserves as devices whatever the extension
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private val WINDOWS_RESERVED_REGEX = Regex("^(?i:con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?$")

    /**
     * Prefix every plugin file lives under, inside the location-addressed root
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private const val PLUGIN_DATA_DIR = "plugin-data"

    private const val MAX_LOCATION_KEY_LENGTH = 1024
    private const val MAX_SEGMENT_LENGTH = 200

    /**
     * Compose the location key of a plugin file
     *
     * @param pluginId Plugin ID
     * @param path Relative path inside the plugin's namespace
     * @return Location key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginKey(pluginId: String, path: String): String {
        require(PluginDescriptor.PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$PLUGIN_DATA_DIR/$pluginId/${validatePath(path)}"
    }

    /**
     * Compose the public reference of a plugin file
     *
     * @param pluginId Plugin ID
     * @param path Relative path inside the plugin's namespace
     * @return Public reference
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginReference(pluginId: String, path: String): String {
        require(PluginDescriptor.PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$pluginId/${validatePath(path)}"
    }

    /**
     * Compose the location key every file of a plugin lives under
     *
     * @param pluginId Plugin ID
     * @return Location key prefix
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pluginBaseKey(pluginId: String): String {
        require(PluginDescriptor.PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        return "$PLUGIN_DATA_DIR/$pluginId"
    }

    /**
     * Validate a location key
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
     * Banning the backslash keeps `..\..\x` from being reinterpreted as three segments
     * by `Path.resolve` on Windows.
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
