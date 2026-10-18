package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.sdk.plugin.PluginLibraryEntry
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryIndex
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider
import kotlin.io.path.extension
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Plugin library registry tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginLibraryRegistry
 */
class PluginLibraryRegistryTests {
    private val directory: Path = Files.createTempDirectory("plugin-libs-test")
    private val registry = PluginLibraryRegistry()

    @AfterTest
    fun cleanUp() {
        registry.close()
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `identical libraries share one loader and resolve dependencies`() {
        val libraries = fixtureLibraries()
        val first = registry.acquire(libraries.installed)
        val second = registry.acquire(libraries.installed)
        try {
            assertEquals(first.loaders.size, second.loaders.size)
            first.loaders.indices.forEach { index ->
                assertSame(first.loaders[index], second.loaders[index])
            }

            val plugin = PluginClassLoaderManager.create(
                libraries.pluginJar.toUri().toURL(),
                first.loaders
            )
            try {
                assertEquals("BAP", plugin.loadClass("plugin.P").getMethod("value").invoke(null))

                first.release()
                // The second set still holds the same shared loaders.
                assertEquals("BAP", plugin.loadClass("plugin.P").getMethod("value").invoke(null))
            } finally {
                plugin.close()
            }
        } finally {
            first.release()
            second.release()
        }
    }

    @Test
    fun `a released library is not reused`() {
        val libraries = fixtureLibraries()
        val first = registry.acquire(libraries.installed)
        val firstLoaders = first.loaders.toList()
        first.release()

        val second = registry.acquire(libraries.installed)
        try {
            second.loaders.indices.forEach { index ->
                assertNotSame(firstLoaders[index], second.loaders[index])
            }
        } finally {
            second.release()
        }
    }

    @Test
    fun `the same bytes with a different dependency closure get a different loader`() {
        val libraries = fixtureLibraries()
        val digestA = libraries.digestA
        val withDependency = registry.acquire(libraries.installed)
        val withoutDependency = registry.acquire(
            InstalledPluginLibraries(
                PluginLibraryIndex(
                    libraries = listOf(
                        PluginLibraryEntry(digestA, "a.jar", listOf("test:a:1")),
                        libraries.entryB
                    )
                ),
                libraries.installed.paths
            )
        )
        try {
            val first = withDependency.libraries.first { it.key.sha256 == digestA }.loader
            val second = withoutDependency.libraries.first { it.key.sha256 == digestA }.loader
            assertNotSame(first, second)
        } finally {
            withDependency.release()
            withoutDependency.release()
        }
    }

    private fun fixtureLibraries(): FixtureLibraries {
        val bJar = compileJar(
            "b.jar",
            mapOf("fixture/B.java" to "package fixture; public class B { public static String value() { return \"B\"; } }"),
            emptyList()
        )
        val aJar = compileJar(
            "a.jar",
            mapOf("fixture/A.java" to "package fixture; public class A { public static String value() { return B.value() + \"A\"; } }"),
            listOf(bJar)
        )
        val pluginJar = compileJar(
            "plugin.jar",
            mapOf("plugin/P.java" to "package plugin; public class P { public static String value() { return fixture.A.value() + \"P\"; } }"),
            listOf(aJar, bJar)
        )
        val digestA = sha256(aJar)
        val digestB = sha256(bJar)
        val entryA = PluginLibraryEntry(digestA, "a.jar", listOf("test:a:1"), listOf(digestB))
        val entryB = PluginLibraryEntry(digestB, "b.jar", listOf("test:b:1"))
        val installed = InstalledPluginLibraries(
            PluginLibraryIndex(libraries = listOf(entryA, entryB)),
            mapOf(digestA to aJar, digestB to bJar)
        )
        return FixtureLibraries(installed, digestA, entryB, pluginJar)
    }

    private fun compileJar(name: String, sources: Map<String, String>, classpath: List<Path>): Path {
        val work = Files.createTempDirectory(directory, "compile-")
        val sourceRoot = work.resolve("src")
        val classRoot = work.resolve("classes")
        Files.createDirectories(classRoot)
        val files = sources.map { (path, source) ->
            val file = sourceRoot.resolve(path)
            Files.createDirectories(file.parent)
            Files.writeString(file, source)
            file.toString()
        }
        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("A JDK compiler is required for this test")
        val options = buildList {
            add("-d")
            add(classRoot.toString())
            if (classpath.isNotEmpty()) {
                add("-cp")
                add(classpath.joinToString(File.pathSeparator))
            }
        }
        val result = compiler.run(null, null, null, *options.toTypedArray(), *files.toTypedArray())
        check(result == 0) { "javac failed with exit code $result" }

        val jar = directory.resolve(name)
        JarOutputStream(Files.newOutputStream(jar)).use { output ->
            Files.walk(classRoot).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.extension == "class" }.forEach { file ->
                    val entryName = classRoot.relativize(file).toString().replace('\\', '/')
                    output.putNextEntry(JarEntry(entryName))
                    Files.copy(file, output)
                    output.closeEntry()
                }
            }
        }
        return jar
    }

    private fun sha256(file: Path): String =
        MessageDigest.getInstance("SHA-256")
            .digest(Files.readAllBytes(file))
            .joinToString("") { "%02x".format(it) }

    private data class FixtureLibraries(
        val installed: InstalledPluginLibraries,
        val digestA: String,
        val entryB: PluginLibraryEntry,
        val pluginJar: Path
    )
}
