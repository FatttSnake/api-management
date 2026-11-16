package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import top.fatweb.apimanagement.plugin.signing.PluginSigner
import java.io.File

/**
 * Stages the plugin public key as `META-INF/plugin.pub.pem` (into the generated
 * resource root) so it is embedded in the jar and the gateway can verify the jar.
 *
 * When an external private key is configured, the public key is derived from it;
 * otherwise the task uses `keys/public.pem`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Copies a developer public key into the build output")
abstract class PreparePluginPubKey : DefaultTask() {
    @get:Internal
    abstract val keysDir: DirectoryProperty

    /**
     * Optional external private key. When set, `keys/` is not consulted.
     */
    @get:Internal
    abstract val privateKeyFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        val privateKey = privateKeyFile.orNull?.asFile
        if (privateKey != null) {
            require(privateKey.isFile) {
                "Signing key $privateKey does not exist"
            }
            out.writeText(PluginSigner.publicKeyPemFromPrivate(privateKey.readText()))
            return
        }
        val publicFile = File(keysDir.get().asFile, "public.pem")
        require(publicFile.exists()) {
            "keys/public.pem not found — configure apiPlugin.signingKeyFile / " +
                "API_PLUGIN_SIGNING_KEY_FILE, or run ./gradlew genPluginKeys first"
        }
        out.writeText(publicFile.readText())
    }
}
