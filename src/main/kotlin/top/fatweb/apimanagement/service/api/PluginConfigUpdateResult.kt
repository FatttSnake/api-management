package top.fatweb.apimanagement.service.api

/**
 * Outcome of an administrator plugin-config save
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginConfigUpdateResult(
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: String,
    /**
     * Declared keys whose effective value changed; empty when the save wrote the same values
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val changedKeys: Set<String>,
)
