package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Resolves the plugin's runtime dependencies into `META-INF/api-plugin-deps.json`.
 *
 * The gateway never resolves versions at runtime: this task records the exact coordinate,
 * digest, size, direct dependency edges and shared-loader groups of every module, and copies
 * the module jars into [modulesDirectory] so `deployPlugin` can upload them.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@CacheableTask
abstract class GeneratePluginLock : DefaultTask() {
    /**
     * Resolved runtime classpath.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * Artifact file path to `group:name:version`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val artifactModules: MapProperty<String, String>

    /**
     * Module dependency edges, one `from=to` per entry.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val dependencyEdges: ListProperty<String>

    /**
     * Groups of `group:artifact` values that must share one class loader.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val sharedLoaderGroups: ListProperty<List<String>>

    /**
     * Lock file.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Directory receiving one `<sha256>.jar` per module.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:OutputDirectory
    abstract val modulesDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val modulesDir = modulesDirectory.get().asFile
        modulesDir.deleteRecursively()
        modulesDir.mkdirs()

        val modulesByPath = artifactModules.get()
        val artifacts =
            classpath.files
                .asSequence()
                .filter { it.isFile }
                .mapNotNull { file ->
                    val coordinate = modulesByPath[file.absolutePath] ?: return@mapNotNull null
                    if (isExcluded(coordinate)) null else coordinate to Artifact(coordinate, file)
                }.associate { (coordinate, artifact) -> coordinate to artifact }

        val byDigest = artifacts.values.associateBy { it.sha256 }
        byDigest.values.forEach { module ->
            Files.copy(
                module.file.toPath(),
                modulesDir.resolve("${module.sha256}.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        val gatewayProvided =
            modulesByPath.values
                .filter(::isExcluded)
                .mapNotNull { coordinate ->
                    val parts = coordinate.split(':')
                    if (parts.size == 3) "${parts[0]}:${parts[1]}" else null
                }.toSet()
        val document =
            PluginLockAssembler.assemble(
                artifacts =
                artifacts.values.map { module ->
                    LockArtifact(
                        group = module.group,
                        name = module.name,
                        version = module.version,
                        sha256 = module.sha256,
                        size = module.file.length(),
                    )
                },
                edges = dependencyEdges.get(),
                sharedLoaders = sharedLoaderGroups.get(),
                gatewayProvided = gatewayProvided,
            )

        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(PluginLockAssembler.toJson(document))
        logger.lifecycle(
            "Locked {} plugin module{} ({} file{}, {} shared loader group{})",
            document.modules.size,
            if (document.modules.size == 1) "" else "s",
            byDigest.size,
            if (byDigest.size == 1) "" else "s",
            document.groups.size,
            if (document.groups.size == 1) "" else "s",
        )
    }

    private fun isExcluded(coordinate: String): Boolean {
        if (coordinate.startsWith("project:") || coordinate.startsWith("file:")) {
            return true
        }
        val parts = coordinate.split(':')
        if (parts.size != 3) {
            return true
        }
        val (group, name, _) = parts
        if (group == "top.fatweb" && name == "api-management-gateway-api") {
            return true
        }
        if (group == "org.jetbrains" && name == "annotations") {
            return true
        }
        return group == "org.jetbrains.kotlin" && name.startsWith("kotlin-stdlib")
    }

    private class Artifact(coordinate: String, val file: File) {
        val parts: List<String> = coordinate.split(':')
        val group: String = parts[0]
        val name: String = parts[1]
        val version: String = parts[2]
        val sha256: String = sha256(file)
    }

    private companion object {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) {
                        break
                    }
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
        }
    }
}
