package top.fatweb.apimanagement.component.plugin

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryIndex
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Content-addressed cache for plugin dependency jars
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginLibraryCache(private val serverProperties: ServerProperties) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val lock = Any()

    /**
     * Read a plugin's library index and materialize its nested jars
     *
     * Must be called after the plugin jar's signature has been verified.
     *
     * @param jarPath Verified plugin jar
     * @return Installed libraries
     * @throws IllegalArgumentException when the index or a nested jar is malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun install(jarPath: Path): InstalledPluginLibraries = synchronized(lock) {
        JarFile(jarPath.toFile()).use { jarFile ->
            val indexEntry = jarFile.getJarEntry(PluginLibraryIndex.INDEX_ENTRY)
                ?: return InstalledPluginLibraries(PluginLibraryIndex(), emptyMap())
            val json = jarFile.getInputStream(indexEntry).use { it.readBytes().toString(Charsets.UTF_8) }
            val index = PluginLibraryIndex.read(json)
            require(index.libraries.size <= MAX_LIBRARIES) {
                "Plugin bundles ${index.libraries.size} libraries, the maximum is $MAX_LIBRARIES"
            }
            val directory = Path.of(serverProperties.storage.pluginLibraryDir)
            Files.createDirectories(directory)
            val paths = index.libraries.associate { library ->
                val entry = jarFile.getJarEntry(PluginLibraryIndex.entryPath(library.sha256))
                    ?: throw IllegalArgumentException(
                        "Plugin library index references a missing jar: ${library.sha256}"
                    )
                library.sha256 to materialize(jarFile, entry, directory, library.sha256)
            }
            logger.debug("Materialized {} plugin libraries from {}", paths.size, jarPath.fileName)
            InstalledPluginLibraries(index, paths)
        }
    }

    private fun materialize(
        jarFile: JarFile,
        entry: JarEntry,
        directory: Path,
        sha256: String
    ): Path {
        val target = directory.resolve("$sha256.jar")
        if (Files.isRegularFile(target)) return target

        val temporary = Files.createTempFile(directory, ".plugin-lib-", ".tmp")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            jarFile.getInputStream(entry).use { input ->
                Files.newOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual == sha256) { "Plugin library ${entry.name} failed its digest check" }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return target
    }

    companion object {
        private const val MAX_LIBRARIES = 512
    }
}

/**
 * Libraries installed from one plugin jar
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class InstalledPluginLibraries(
    /**
     * Parsed library index
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val index: PluginLibraryIndex,

    /**
     * Materialized jar path by digest
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val paths: Map<String, Path>
)
