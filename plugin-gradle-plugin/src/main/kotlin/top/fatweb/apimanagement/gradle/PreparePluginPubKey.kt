package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Stages `keys/public.pem` as `META-INF/plugin.pub.pem` (into the generated
 * resource root) so it is embedded in the jar and the gateway can verify the jar.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Copies a local developer public key into the build output")
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
