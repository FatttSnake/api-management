package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Generates `META-INF/api-plugin.json` from the [ApiPluginExtension] into
 * `build/generated/api-plugin/META-INF/`, from where the `jar` task picks it up.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@CacheableTask
abstract class GeneratePluginDescriptor : DefaultTask() {
    @get:Input
    abstract val pluginId: Property<String>

    @get:Input
    abstract val pluginName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val pluginDescription: Property<String>

    @get:Input
    abstract val author: Property<String>

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val requiresGateway: Property<String>

    @get:Input
    abstract val requiresGatewayApi: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val id = pluginId.get()
        require(PLUGIN_ID.matches(id)) {
            "apiPlugin.pluginId must match ${PLUGIN_ID.pattern} (got \"$id\")"
        }
        val name = pluginName.get()
        require(name.isNotBlank()) { "apiPlugin.pluginName must not be blank" }
        val code =
            versionCode.orNull
                ?: error(
                    "apiPlugin.versionCode is REQUIRED — set it explicitly in the apiPlugin { } block " +
                        "(e.g. versionCode = 1).",
                )
        require(code >= 1) { "apiPlugin.versionCode must be >= 1 (got $code)" }
        val main = mainClass.orNull?.trim().orEmpty()
        require(main.isNotBlank()) {
            "apiPlugin.mainClass is required and must name the ApiPlugin implementation"
        }
        require(requiresGateway.get().isNotBlank()) { "apiPlugin.requiresGateway must not be blank" }
        require(requiresGatewayApi.get().isNotBlank()) { "apiPlugin.requiresGatewayApi must not be blank" }

        val payload =
            linkedMapOf<String, Any>(
                "format" to 1,
                "pluginId" to id,
                "name" to name,
                "versionName" to versionName.get(),
                "versionCode" to code,
                "description" to pluginDescription.get(),
                "author" to author.get(),
                "mainClass" to main,
                "requiresGateway" to requiresGateway.get(),
                "requiresGatewayApi" to requiresGatewayApi.get(),
                "dependencies" to emptyMap<String, String>(),
            )
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(jacksonObjectMapper().writeValueAsString(payload))
        logger.lifecycle("Wrote plugin descriptor: {}", out)
    }

    companion object {
        private val PLUGIN_ID = Regex("^[a-z][a-z0-9-]{0,63}$")
    }
}
