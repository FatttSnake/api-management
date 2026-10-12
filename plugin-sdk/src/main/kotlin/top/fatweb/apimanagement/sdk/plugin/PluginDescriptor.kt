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
     * Unique plugin ID, matching [PLUGIN_ID_REGEX]
     *
     * The single source of the plugin's identity: it namespaces every endpoint under
     * `/api/{plugin}/v{version}/...` and is what every `plugin_id` column in the gateway
     * references. Controllers do not repeat it - `@ApiController` carries only the API
     * version and sub-path.
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
     * version to allow an upgrade, and at least 1
     *
     * Required in a hand-written descriptor, and has no default: it is what decides whether an
     * install may replace one already present, so a value nobody chose would be a value nobody
     * can reason about. The Gradle plugin takes it from `apiPlugin { versionCode }` and refuses
     * to build without it.
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
         * Lives here, in the one artifact both sides of a plugin build depend on, because it is
         * the one rule they have to agree on: the Gradle plugin refuses at build time what the
         * gateway would refuse at install time, and two copies of it that drifted would let a
         * plugin build and then fail to install.
         *
         * The upper bound of 64 is not cosmetic: it is the width of `t_b_api_plugin.plugin_id`
         * (and of every `plugin_id` column that references it), so an ID that passed a shorter
         * pattern would be accepted by the gateway and then be truncated - or refused outright -
         * by the insert, leaving the row disagreeing with the ID the jar declares. Kept in the
         * pattern rather than checked separately so both sides cannot drift apart.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val PLUGIN_ID_REGEX = Regex("^[a-z][a-z0-9-]{0,63}$")
    }
}
