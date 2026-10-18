package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import top.fatweb.apimanagement.sdk.plugin.PluginClasspath
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryEntry
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryIndex
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Bundles a plugin's runtime dependencies as nested jars
 *
 * Writes `META-INF/api-plugin-libs/index.json` and one `<sha256>.jar` per dependency into
 * [outputDirectory], from where the `jar` task picks them up.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class BundlePluginLibraries : DefaultTask() {
    /**
     * Resolved runtime classpath
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * Artifact file path to module coordinate, e.g. `org.bouncycastle:bcprov-jdk18on:1.86`
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val artifactModules: MapProperty<String, String>

    /**
     * Module dependency edges, one `from=to` per entry
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val dependencyEdges: ListProperty<String>

    /**
     * Gateway-provided dependency patterns
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val providedPatterns: ListProperty<String>

    /**
     * Whether dependencies are bundled at all
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val bundleDependencies: Property<Boolean>

    /**
     * Whether the plugin bundles its own BouncyCastle
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val bundleBouncyCastle: Property<Boolean>

    /**
     * Generated library directory
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val root = outputDirectory.get().asFile
        root.deleteRecursively()
        root.mkdirs()
        if (!bundleDependencies.get()) {
            logger.lifecycle("Dependency bundling is disabled, the plugin jar stays thin")
            return
        }

        val patterns = providedPatterns.get()
        val modulesByPath = artifactModules.get()
        val artifacts = classpath.files
            .asSequence()
            .filter { it.isFile }
            .mapNotNull { file ->
                val module = modulesByPath[file.absolutePath] ?: return@mapNotNull null
                if (isProvided(module, patterns)) null else Artifact(file, module)
            }
            .toList()

        val byDigest = linkedMapOf<String, MutableList<Artifact>>()
        artifacts.forEach { artifact ->
            byDigest.getOrPut(sha256(artifact.file)) { mutableListOf() }.add(artifact)
        }

        val libraryRoot = root.resolve(PluginLibraryIndex.LIBRARY_DIR)
        libraryRoot.mkdirs()
        byDigest.forEach { (digest, copies) ->
            Files.copy(
                copies.first().file.toPath(),
                libraryRoot.resolve("$digest.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        val coordinateToDigest = mutableMapOf<String, String>()
        byDigest.forEach { (digest, copies) ->
            copies.forEach { coordinateToDigest[it.module] = digest }
        }
        val edges = parseEdges()
        val libraries = byDigest.map { (digest, copies) ->
            val dependencies = copies
                .flatMap { edges[it.module].orEmpty() }
                .mapNotNull { coordinateToDigest[it] }
                .filter { it != digest }
                .distinct()
                .sorted()
            PluginLibraryEntry(
                sha256 = digest,
                file = copies.first().file.name,
                modules = copies.map { it.module }.sorted(),
                dependsOn = dependencies
            )
        }.sortedBy { it.sha256 }

        if (bundleBouncyCastle.get()) {
            validateBouncyCastle(libraries)
        }

        val index = PluginLibraryIndex(libraries = libraries)
        libraryRoot.resolve("index.json")
            .writeText(PluginLibraryIndex.write(index))
        logger.lifecycle(
            "Bundled {} plugin librar{} ({} nested jar{})",
            libraries.size,
            if (libraries.size == 1) "y" else "ies",
            byDigest.size,
            if (byDigest.size == 1) "" else "s"
        )
    }

    private fun isProvided(module: String, patterns: List<String>): Boolean {
        if (module.startsWith("project:")) return false
        val parts = module.split(':')
        if (parts.size < 2) return false
        return patterns.any { PluginClasspath.matches(it, parts[0], parts[1]) }
    }

    private fun parseEdges(): Map<String, List<String>> {
        val edges = mutableMapOf<String, MutableList<String>>()
        dependencyEdges.get().forEach { edge ->
            val separator = edge.indexOf('=')
            if (separator <= 0) return@forEach
            val from = edge.substring(0, separator)
            val to = edge.substring(separator + 1)
            edges.getOrPut(from) { mutableListOf() }.add(to)
        }
        return edges
    }

    private fun validateBouncyCastle(libraries: List<PluginLibraryEntry>) {
        val names = libraries
            .flatMap { it.modules }
            .mapNotNull { module ->
                val parts = module.split(':')
                if (parts.size >= 2 && parts[0] == PluginClasspath.BOUNCY_CASTLE_GROUP) parts[1] else null
            }
            .toSet()
        if ("bcpkix-jdk18on" in names) {
            val missing = listOf("bcprov-jdk18on", "bcutil-jdk18on").filterNot { it in names }
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "apiPlugin.bundleBouncyCastle is enabled, but the resolved BouncyCastle " +
                        "closure is incomplete: missing ${missing.joinToString()}"
                )
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class Artifact(val file: File, val module: String)
}
