package top.fatweb.apimanagement.sdk.plugin

/**
 * Plugin descriptor
 *
 * Parsed from `META-INF/api-plugin.json` inside the plugin jar.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginDescriptor(
    /**
     * Unique plugin ID, matching [PLUGIN_ID_REGEX]
     *
     * Namespaces every endpoint and is what every `plugin_id` column in the gateway references.
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
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionName: String,

    /**
     * Monotonic integer version, at least 1
     *
     * A version greater than the installed one is an upgrade, and an upgrade takes the old
     * version out of the gateway on the way to the new one. Re-installing the version that is
     * already there is allowed and replaces the code in place, which is what a development loop
     * does over and over; only a version older than the installed one is refused.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionCode: Int,

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
) {
    companion object {
        /**
         * Pattern every plugin ID must match
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val PLUGIN_ID_REGEX = Regex("^[a-z][a-z0-9-]{0,63}$")
    }
}
