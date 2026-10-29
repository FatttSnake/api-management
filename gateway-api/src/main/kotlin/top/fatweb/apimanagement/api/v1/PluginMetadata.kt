package top.fatweb.apimanagement.api.v1

/**
 * Runtime metadata for the calling plugin.
 *
 * @property pluginId plugin ID
 * @property pluginVersion plugin version name
 * @property gatewayVersion gateway version
 * @property apiVersion API major version implemented by the plugin
 * @property gatewayApiVersion exact gateway API version
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginMetadata @JvmOverloads constructor(
    val pluginId: String,
    val pluginVersion: String = "",
    val gatewayVersion: String = "",
    val apiVersion: Int = 1,
    val gatewayApiVersion: String = "",
) {
    init {
        require(pluginId.isNotBlank()) { "pluginId must not be blank" }
    }
}
