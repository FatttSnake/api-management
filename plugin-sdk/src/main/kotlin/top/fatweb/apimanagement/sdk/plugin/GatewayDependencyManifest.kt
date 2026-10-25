package top.fatweb.apimanagement.sdk.plugin

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Versions of the dependencies the gateway provides
 *
 * Embedded in the plugin SDK at [RESOURCE] so a plugin build can check what it resolves
 * against what will actually be on the gateway's classpath at runtime.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class GatewayDependencyManifest(
    /**
     * Manifest format version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val format: Int = FORMAT,

    /**
     * SDK version the manifest was generated with
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sdkVersion: String? = null,

    /**
     * Provided dependency versions, keyed by `group:name`
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val dependencies: Map<String, String> = emptyMap()
) {
    /**
     * Version the gateway provides for a module
     *
     * @param module Module coordinate without its version, e.g. `org.flywaydb:flyway-core`
     * @return Provided version, or null when the manifest does not list the module
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun versionOf(module: String): String? = dependencies[module]

    companion object {
        /**
         * Current manifest format
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val FORMAT = 1

        /**
         * Classpath resource of the manifest
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val RESOURCE = "META-INF/api-management-gateway-dependencies.json"

        /**
         * Parse a manifest
         *
         * @param json Manifest JSON
         * @return Parsed manifest
         * @throws IllegalArgumentException when the manifest is malformed
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun read(json: String): GatewayDependencyManifest {
            val manifest = try {
                jacksonObjectMapper().readValue<GatewayDependencyManifest>(json)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid gateway dependency manifest: ${e.message}", e)
            }
            require(manifest.format == FORMAT) {
                "Unsupported gateway dependency manifest format: ${manifest.format}"
            }
            manifest.dependencies.forEach { (module, version) ->
                require(module.contains(':')) {
                    "Gateway dependency manifest has an invalid module coordinate: '$module'"
                }
                require(version.isNotBlank()) {
                    "Gateway dependency manifest has no version for '$module'"
                }
            }
            return manifest
        }
    }
}
