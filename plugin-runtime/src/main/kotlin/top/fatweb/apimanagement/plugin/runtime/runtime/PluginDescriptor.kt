package top.fatweb.apimanagement.plugin.runtime.runtime

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import top.fatweb.apimanagement.api.v1.version.VersionRange
import top.fatweb.apimanagement.api.v1.version.VersionRangeException
import java.nio.charset.StandardCharsets

/**
 * Parsed `META-INF/api-plugin.json`.
 *
 * @property format descriptor format
 * @property pluginId plugin ID
 * @property name display name
 * @property versionName human-readable version
 * @property versionCode monotonic version
 * @property mainClass [top.fatweb.apimanagement.api.v1.ApiPlugin] implementation
 * @property apiVersion gateway API major version, derived from the minimum of
 *                    [requiresGatewayApi]
 * @property requiresGateway gateway version range
 * @property requiresGatewayApi gateway API version range
 * @property description description
 * @property author author
 * @property dependencies declared dependency ranges, keyed by `group:name`
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginDescriptor(
    val format: Int,
    val pluginId: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val mainClass: String,
    val apiVersion: Int,
    val requiresGateway: String,
    val requiresGatewayApi: String,
    val description: String = "",
    val author: String = "",
    val dependencies: Map<String, String> = emptyMap()
)

/**
 * Parses and validates plugin descriptors.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginDescriptorParser {
    private val mapper: JsonMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addModules(KotlinModule.Builder().build())
        .build()

    /**
     * Parses a descriptor.
     *
     * @param bytes JSON bytes
     * @return descriptor
     * @throws PluginDescriptorException when the descriptor is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parse(bytes: ByteArray): PluginDescriptor {
        val json = try {
            mapper.readValue(bytes, DescriptorJson::class.java)
        } catch (ex: Exception) {
            throw PluginDescriptorException("Cannot parse plugin descriptor: ${ex.message}")
        }
        val apiVersion = try {
            VersionRange.parse(json.requiresGatewayApi).minimumMajor()
        } catch (ex: VersionRangeException) {
            throw PluginDescriptorException("Invalid requiresGatewayApi: ${ex.message}")
        }
        val descriptor = PluginDescriptor(
            format = json.format,
            pluginId = json.pluginId,
            name = json.name,
            versionName = json.versionName,
            versionCode = json.versionCode,
            mainClass = json.mainClass,
            apiVersion = apiVersion,
            requiresGateway = json.requiresGateway,
            requiresGatewayApi = json.requiresGatewayApi,
            description = json.description,
            author = json.author,
            dependencies = json.dependencies
        )
        validate(descriptor)
        return descriptor
    }

    /**
     * Parses a descriptor.
     *
     * @param text JSON text
     * @return descriptor
     * @throws PluginDescriptorException when the descriptor is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parse(text: String): PluginDescriptor = parse(text.toByteArray(StandardCharsets.UTF_8))

    private fun validate(descriptor: PluginDescriptor) {
        if (descriptor.format != FORMAT) {
            throw PluginDescriptorException("Unsupported plugin descriptor format: ${descriptor.format}")
        }
        if (!PLUGIN_ID.matches(descriptor.pluginId)) {
            throw PluginDescriptorException("Invalid pluginId: ${descriptor.pluginId}")
        }
        if (descriptor.name.isBlank() || descriptor.versionName.isBlank()) {
            throw PluginDescriptorException("Plugin name and versionName must not be blank")
        }
        if (descriptor.versionCode < 1) {
            throw PluginDescriptorException("versionCode must be at least 1")
        }
        if (descriptor.mainClass.isBlank()) {
            throw PluginDescriptorException("mainClass must not be blank")
        }
        if (descriptor.apiVersion < 1) {
            throw PluginDescriptorException("apiVersion must be at least 1")
        }
        if (descriptor.requiresGateway.isBlank() || descriptor.requiresGatewayApi.isBlank()) {
            throw PluginDescriptorException("requiresGateway and requiresGatewayApi must not be blank")
        }
        descriptor.dependencies.forEach { (coordinate, range) ->
            if (coordinate.isBlank() || !coordinate.contains(':') || range.isBlank()) {
                throw PluginDescriptorException("Invalid dependency declaration: $coordinate = $range")
            }
        }
    }

    private data class DescriptorJson(
        val format: Int = 0,
        val pluginId: String = "",
        val name: String = "",
        val versionName: String = "",
        val versionCode: Int = 0,
        val mainClass: String = "",
        val requiresGateway: String = "",
        val requiresGatewayApi: String = "",
        val description: String = "",
        val author: String = "",
        val dependencies: Map<String, String> = emptyMap()
    )

    private companion object {
        const val FORMAT = 1
        val PLUGIN_ID = Regex("^[a-z][a-z0-9-]{0,63}$")
    }
}

/**
 * Invalid plugin descriptor.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginDescriptorException(message: String) : RuntimeException(message)
