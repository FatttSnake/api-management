package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin descriptor
 *
 * Parsed from `META-INF/api-plugin.json` inside the plugin jar. Holds the
 * Android-style package version: [versionCode] must strictly increase on upgrade,
 * while [versionName] is a human-readable version string.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginDescriptor(
    /**
     * Unique plugin ID, must match the plugin id of every @ApiController in the jar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: String,

    /**
     * Plugin display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val name: String,

    /**
     * Human-readable version, e.g. "1.2.0"
     *
     * Required in a hand-written descriptor — a plugin must always know which
     * version it is. When using the Gradle plugin it is filled from the project
     * `version` if not set in `apiPlugin { }`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionName: String,

    /**
     * Monotonic integer version; must be greater than the currently installed
     * version to allow an upgrade
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionCode: Int = 1,

    /**
     * Plugin description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val description: String = "",

    /**
     * Plugin author
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val author: String = "",

    /**
     * Fully-qualified class name of the [PluginLifecycle] implementation, if any
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val mainClass: String? = null
)
