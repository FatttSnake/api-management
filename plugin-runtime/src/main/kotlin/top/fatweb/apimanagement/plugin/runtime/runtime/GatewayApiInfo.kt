package top.fatweb.apimanagement.plugin.runtime.runtime

import java.util.Properties

/**
 * Version metadata embedded in the gateway API jar.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object GatewayApiInfo {
    /**
     * Exact gateway API version.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val version: String by lazy { readVersion() }

    /**
     * Gateway API major version.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val major: Int by lazy {
        version.substringBefore('.').toIntOrNull()
            ?: error("Invalid gateway API version: $version")
    }

    private fun readVersion(): String {
        val stream =
            GatewayApiInfo::class.java.classLoader
                .getResourceAsStream("META-INF/gateway-api.properties")
                ?: error("META-INF/gateway-api.properties is missing from gateway-api")
        return stream.use { input ->
            val properties = Properties()
            properties.load(input)
            properties.getProperty("version")?.takeIf { it.isNotBlank() }
                ?: error("gateway-api.properties has no version")
        }
    }
}
