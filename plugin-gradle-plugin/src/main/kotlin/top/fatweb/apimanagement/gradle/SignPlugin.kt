package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import top.fatweb.apimanagement.plugin.signing.PluginSigner

/**
 * Signs the produced jar in place with `keys/private.pem` and embeds the Ed25519
 * signature as `META-INF/plugin.sig`. Runs after every `jar`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Rewrites the jar in place with a signature that depends on the local private key")
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
