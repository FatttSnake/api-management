package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.properties.StorageProperties
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryEntry
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryIndex
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Plugin library cache tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginLibraryCache
 */
class PluginLibraryCacheTests {
    private val directory: Path = Files.createTempDirectory("plugin-library-cache-test")
    private val cache = PluginLibraryCache(
        ServerProperties(storage = StorageProperties(pluginLibraryDir = directory.resolve("cache").toString()))
    )

    @AfterTest
    fun cleanUp() {
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `nested jars are materialized once by digest`() {
        val library = jarBytes()
        val digest = sha256(library)
        val plugin = pluginJar(
            index = PluginLibraryIndex(
                libraries = listOf(PluginLibraryEntry(digest, "library.jar", listOf("test:library:1")))
            ),
            libraries = mapOf(digest to library)
        )

        val first = cache.install(plugin)
        val second = cache.install(plugin)

        assertEquals(first.paths, second.paths)
        assertContentEquals(library, Files.readAllBytes(first.paths.getValue(digest)))
    }

    @Test
    fun `a digest mismatch refuses the install`() {
        val library = jarBytes()
        val wrongDigest = "0".repeat(64)
        val plugin = pluginJar(
            index = PluginLibraryIndex(
                libraries = listOf(PluginLibraryEntry(wrongDigest, "library.jar", listOf("test:library:1")))
            ),
            libraries = mapOf(wrongDigest to library)
        )

        assertFailsWith<IllegalArgumentException> { cache.install(plugin) }
    }

    @Test
    fun `a missing nested jar refuses the install`() {
        val digest = sha256(jarBytes())
        val plugin = pluginJar(
            index = PluginLibraryIndex(
                libraries = listOf(PluginLibraryEntry(digest, "library.jar", listOf("test:library:1")))
            ),
            libraries = emptyMap()
        )

        assertFailsWith<IllegalArgumentException> { cache.install(plugin) }
    }

    private fun pluginJar(
        index: PluginLibraryIndex,
        libraries: Map<String, ByteArray>
    ): Path {
        val path = directory.resolve("plugin-${System.nanoTime()}.jar")
        JarOutputStream(Files.newOutputStream(path)).use { output ->
            output.putNextEntry(JarEntry(PluginLibraryIndex.INDEX_ENTRY))
            output.write(PluginLibraryIndex.write(index).toByteArray(Charsets.UTF_8))
            output.closeEntry()
            libraries.forEach { (digest, bytes) ->
                output.putNextEntry(JarEntry(PluginLibraryIndex.entryPath(digest)))
                output.write(bytes)
                output.closeEntry()
            }
        }
        return path
    }

    private fun jarBytes(): ByteArray {
        val path = directory.resolve("library-${System.nanoTime()}.jar")
        JarOutputStream(Files.newOutputStream(path)).use { output ->
            output.putNextEntry(JarEntry("fixture/Library.class"))
            output.write(byteArrayOf(1, 2, 3))
            output.closeEntry()
        }
        return Files.readAllBytes(path)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
