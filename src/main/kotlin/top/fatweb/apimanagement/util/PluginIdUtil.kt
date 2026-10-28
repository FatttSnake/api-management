package top.fatweb.apimanagement.util

/**
 * Plugin ID validation shared by plugin-adjacent utilities.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginIdUtil {
    /**
     * Plugin ID pattern, matching the gateway contract.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val REGEX: Regex = Regex("^[a-z][a-z0-9-]{0,63}$")
}
