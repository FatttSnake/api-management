package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import top.fatweb.apimanagement.sdk.plugin.PluginSigner
import java.io.File

/**
 * Generates `META-INF/api-plugin.json` from the [ApiPluginExtension] into
 * `build/generated/api-plugin/META-INF/`, from where the `jar` task picks it up.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class GeneratePluginDescriptor : DefaultTask() {
    @get:Input
    abstract val pluginId: Property<String>

    @get:Input
    abstract val pluginName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Optional
    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val pluginDescription: Property<String>

    @get:Input
    abstract val author: Property<String>

    @get:Optional
    @get:Input
    abstract val mainClass: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val id = pluginId.get()
        require(PLUGIN_ID_REGEX.matches(id)) {
            "apiPlugin.pluginId must match ^[a-z][a-z0-9-]*$ (got \"$id\")"
        }
        val name = pluginName.get()
        require(name.isNotBlank()) { "apiPlugin.pluginName must not be blank" }
        val code = versionCode.orNull
            ?: error(
                "apiPlugin.versionCode is REQUIRED — set it explicitly in the apiPlugin { } block " +
                    "(e.g. versionCode = 1). It no longer defaults to 1, because it drives the " +
                    "gateway's upgrade check and must be bumped on every release."
            )
        require(code >= 1) { "apiPlugin.versionCode must be >= 1 (got $code)" }

        val json = buildString {
            append("{\"pluginId\":").append(json(id))
            append(",\"name\":").append(json(name))
            append(",\"versionName\":").append(json(versionName.get()))
            append(",\"versionCode\":").append(code)
            append(",\"description\":").append(json(pluginDescription.get()))
            append(",\"author\":").append(json(author.get()))
            if (mainClass.isPresent && mainClass.get().isNotBlank()) {
                append(",\"mainClass\":").append(json(mainClass.get()))
            }
            append('}')
        }

        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(json)
        logger.lifecycle("Wrote plugin descriptor: {}", out)
    }

    companion object {
        private val PLUGIN_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

        private fun json(value: String): String = "\"" +
            value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") +
            "\""
    }
}

/**
 * Generates the Ed25519 developer key pair into `keys/` (private.pem + public.pem).
 * Skips silently when `keys/private.pem` already exists — delete it to mint a new
 * identity.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class GenPluginKeys : DefaultTask() {
    @get:Internal
    abstract val keysDir: DirectoryProperty

    @TaskAction
    fun run() {
        val dir = keysDir.get().asFile
        val privateFile = File(dir, "private.pem")
        val publicFile = File(dir, "public.pem")
        if (privateFile.exists()) {
            logger.lifecycle("Keys already exist in {} — reusing them (delete private.pem to regenerate)", dir)
            return
        }
        dir.mkdirs()
        val pair = PluginSigner.generateKeyPair()
        privateFile.writeText(PluginSigner.privateKeyPem(pair.private))
        publicFile.writeText(PluginSigner.publicKeyPem(pair.public))
        logger.lifecycle("Generated Ed25519 keys in {}", dir)
    }
}

/**
 * Stages `keys/public.pem` as `META-INF/plugin.pub.pem` (into the generated
 * resource root) so it is embedded in the jar and the gateway can verify the jar.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class PreparePluginPubKey : DefaultTask() {
    @get:Internal
    abstract val keysDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val publicFile = File(keysDir.get().asFile, "public.pem")
        require(publicFile.exists()) {
            "keys/public.pem not found — run ./gradlew genPluginKeys first"
        }
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(publicFile.readText())
    }
}

/**
 * Signs the produced jar in place with `keys/private.pem` and embeds the Ed25519
 * signature as `META-INF/plugin.sig`. Runs after every `jar`, mirroring the
 * historical `PluginSigner sign` invocation.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class SignPlugin : DefaultTask() {
    @get:Internal
    abstract val jarFile: RegularFileProperty

    @get:Internal
    abstract val privateKeyFile: RegularFileProperty

    @TaskAction
    fun run() {
        val keyFile = privateKeyFile.get().asFile
        require(keyFile.exists()) {
            "$keyFile not found — run ./gradlew genPluginKeys first"
        }
        val jarPath = jarFile.get().asFile.toPath()
        PluginSigner.signAndEmbed(jarPath, keyFile.readText())
    }
}

/**
 * Verifies the signature embedded in the plugin jar against its own
 * `META-INF/plugin.pub.pem`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class VerifyPlugin : DefaultTask() {
    @get:Internal
    abstract val jarFile: RegularFileProperty

    @TaskAction
    fun run() {
        val jarPath = jarFile.get().asFile.toPath()
        if (!PluginSigner.verify(jarPath)) {
            throw GradleException("Signature INVALID for ${jarPath.fileName}")
        }
        logger.lifecycle("Signature valid: {}", jarPath.fileName)
    }
}
