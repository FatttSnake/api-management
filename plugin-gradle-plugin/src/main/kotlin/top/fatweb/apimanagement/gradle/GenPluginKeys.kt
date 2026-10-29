package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import top.fatweb.apimanagement.plugin.signing.PluginSigner
import java.io.File

/**
 * Generates the Ed25519 developer key pair into `keys/` (private.pem + public.pem).
 * Reuses an existing pair, derives a missing `public.pem` from `private.pem`, and
 * refuses to overwrite an existing `public.pem` whose `private.pem` is gone.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Generates developer key material and must never be restored from a cache")
abstract class GenPluginKeys : DefaultTask() {
    @get:Internal
    abstract val keysDir: DirectoryProperty

    @TaskAction
    fun run() {
        val dir = keysDir.get().asFile
        val privateFile = File(dir, "private.pem")
        val publicFile = File(dir, "public.pem")
        when {
            privateFile.exists() && publicFile.exists() ->
                logger.lifecycle("Keys already exist in {} — reusing them (delete private.pem to regenerate)", dir)

            privateFile.exists() -> {
                publicFile.writeText(PluginSigner.publicKeyPemFromPrivate(privateFile.readText()))
                logger.lifecycle("Derived missing public.pem from private.pem in {}", dir)
            }

            publicFile.exists() -> throw GradleException(
                "keys/public.pem exists but keys/private.pem is missing — refusing to overwrite the " +
                    "public key. Restore private.pem, or delete public.pem to mint a new identity.",
            )

            else -> {
                dir.mkdirs()
                val pair = PluginSigner.generateKeyPair()
                privateFile.writeText(PluginSigner.privateKeyPem(pair.private))
                publicFile.writeText(PluginSigner.publicKeyPem(pair.public))
                logger.lifecycle("Generated Ed25519 keys in {}", dir)
            }
        }
    }
}
