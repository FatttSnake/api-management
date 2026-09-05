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
     */
    val pluginId: String,

    /**
     * Plugin display name
     */
    val name: String,

    /**
     * Human-readable version, e.g. "1.2.0"
     */
    val versionName: String = "1.0.0",

    /**
     * Monotonic integer version; must be greater than the currently installed
     * version to allow an upgrade
     */
    val versionCode: Int = 1,

    /**
     * Plugin description
     */
    val description: String = "",

    /**
     * Plugin author
     */
    val author: String = "",

    /**
     * Fully-qualified class name of the [PluginLifecycle] implementation, if any
     */
    val mainClass: String? = null
)
