package top.fatweb.apimanagement.component.plugin

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Plugin class loader tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginClassLoaderManager
 */
class PluginClassLoaderManagerTests {
    private val directory: Path = Files.createTempDirectory("plugin-loader-test")

    @AfterTest
    fun cleanUp() {
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `resources are limited to the plugin jar and bundled libraries`() {
        val gatewayDirectory = directory.resolve("gateway")
        writeFile(gatewayDirectory.resolve("db/migration/V1__gateway.sql"), "gateway")
        writeFile(gatewayDirectory.resolve("same.txt"), "gateway")
        writeFile(gatewayDirectory.resolve("gateway-only.txt"), "gateway")
        writeFile(gatewayDirectory.resolve("META-INF/services/example.Service"), "gateway.Impl")
        val gateway = URLClassLoader(arrayOf(gatewayDirectory.toUri().toURL()), null)

        val pluginJar = directory.resolve("plugin.jar")
        writeJar(
            pluginJar,
            mapOf(
                "db/migration/V1__plugin.sql" to "plugin",
                "same.txt" to "plugin",
                "META-INF/services/example.Service" to "plugin.Impl"
            )
        )
        val libraryJar = directory.resolve("library.jar")
        writeJar(libraryJar, mapOf("library-only.txt" to "library"))

        val library = PluginLibraryClassLoader(libraryJar, emptyList(), gateway)
        val loader = PluginClassLoaderManager.create(
            jarUrl = pluginJar.toUri().toURL(),
            libraries = listOf(library),
            parentFirstPrefixes = emptyList(),
            parent = gateway
        )
        try {
            assertEquals("plugin", loader.getResource("same.txt")?.readText())
            assertEquals("plugin", loader.getResource("db/migration/V1__plugin.sql")?.readText())
            assertEquals("library", loader.getResource("library-only.txt")?.readText())
            assertNull(loader.getResource("gateway-only.txt"))
            assertNull(loader.getResource("db/migration/V1__gateway.sql"))

            assertEquals(
                listOf("plugin"),
                loader.getResources("same.txt").asSequence().map { it.readText() }.toList()
            )
            assertEquals(
                listOf("plugin.Impl"),
                loader.getResources("META-INF/services/example.Service").asSequence().map { it.readText() }.toList()
            )

            assertNull(library.getResource("same.txt"))
            assertNull(library.getResource("gateway-only.txt"))

            val scanned = PathMatchingResourcePatternResolver(loader)
                .getResources("classpath*:db/migration/**")
                .mapNotNull { it.filename }
                .filter { it.isNotBlank() }
                .toSet()
            assertEquals(setOf("V1__plugin.sql"), scanned)

            val previous = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().contextClassLoader = loader
                val viaTccl = PathMatchingResourcePatternResolver()
                    .getResources("classpath*:db/migration/**")
                    .mapNotNull { it.filename }
                    .filter { it.isNotBlank() }
                    .toSet()
                assertEquals(setOf("V1__plugin.sql"), viaTccl)
                assertEquals(0, PathMatchingResourcePatternResolver().getResources("classpath*:gateway-only.txt").size)
            } finally {
                Thread.currentThread().contextClassLoader = previous
            }
            assertNull(loader.getResourceAsStream("gateway-only.txt"))
        } finally {
            loader.close()
            library.close()
            gateway.close()
        }
    }

    @Test
    fun `classes still resolve from the parent`() {
        val pluginJar = directory.resolve("empty-plugin.jar")
        writeJar(pluginJar, mapOf("plugin-only.txt" to "plugin"))

        val loader = PluginClassLoaderManager.create(
            jarUrl = pluginJar.toUri().toURL(),
            parentFirstPrefixes = emptyList(),
            parent = PluginClassLoaderManagerTests::class.java.classLoader
        )
        try {
            assertEquals(
                "org.junit.jupiter.api.Test",
                loader.loadClass("org.junit.jupiter.api.Test").name
            )
            // Spring's metadata readers open classes by resource name, including
            // gateway-provided interfaces and annotations.
            assertNotNull(loader.getResource("org/junit/jupiter/api/Test.class"))
            assertEquals(
                "org.junit.jupiter.api.Test",
                CachingMetadataReaderFactory(loader)
                    .getMetadataReader("org.junit.jupiter.api.Test")
                    .classMetadata.className
            )
            // Batch resource lookups stay isolated: scanning must not see the gateway.
            assertEquals(
                0,
                loader.getResources("org/junit/jupiter/api/Test.class").asSequence().count()
            )
        } finally {
            loader.close()
        }
    }

    private fun writeFile(path: Path, content: String) {
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, content)
    }

    private fun writeJar(path: Path, entries: Map<String, String>) {
        JarOutputStream(Files.newOutputStream(path)).use { output ->
            entries.keys
                .flatMap(::parentDirectories)
                .toSortedSet()
                .forEach { parent ->
                    output.putNextEntry(JarEntry("$parent/"))
                    output.closeEntry()
                }
            entries.forEach { (name, content) ->
                output.putNextEntry(JarEntry(name))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
    }

    private fun parentDirectories(entry: String): List<String> =
        generateSequence(entry.substringBeforeLast('/', "")) { it.substringBeforeLast('/', "") }
            .takeWhile { it.isNotEmpty() }
            .toList()
}
