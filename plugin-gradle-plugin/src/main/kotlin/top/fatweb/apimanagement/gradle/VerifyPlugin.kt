package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import top.fatweb.apimanagement.plugin.signing.PluginSigner

/**
 * Verifies the signature embedded in the plugin jar against its own
 * `META-INF/plugin.pub.pem`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@UntrackedTask(because = "Verifies an artifact and produces no output")
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
