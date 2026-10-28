package top.fatweb.apimanagement.plugin.runtime.classloader

import fixture.a.FixtureA
import fixture.b.FixtureB
import fixture.plugin.FixturePlugin
import org.junit.jupiter.api.io.TempDir
import top.fatweb.apimanagement.plugin.runtime.jarOf
import top.fatweb.apimanagement.plugin.runtime.module.FileSystemPluginModuleStore
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleRegistryTests {
    @TempDir
    lateinit var root: Path

    private lateinit var store: FileSystemPluginModuleStore
    private lateinit var registry: PluginModuleRegistry
    private lateinit var platform: GatewayApiClassLoader

    private lateinit var jarA: ByteArray
    private lateinit var jarB: ByteArray
    private lateinit var shaA: String
    private lateinit var shaB: String

    private fun setUp() {
        store = FileSystemPluginModuleStore(root)
        platform = GatewayApiClassLoader(javaClass.classLoader)
        registry = PluginModuleRegistry(store, platform)

        jarA = jarOf(FixtureA::class)
        jarB = jarOf(FixtureB::class)
        shaA = FileSystemPluginModuleStore.sha256(jarA)
        shaB = FileSystemPluginModuleStore.sha256(jarB)
        store.install(shaA, jarA)
        store.install(shaB, jarB)
    }

    @AfterTest
    fun tearDown() {
        if (::registry.isInitialized) {
            registry.close()
        }
    }

    @Test
    fun `shares one loader for the same module and releases it with the last plugin`() {
        setUp()
        val lock = lockOf(LockedModule("fixture", "a", "1.0.0", shaA))

        val first = registry.acquire(lock)
        val second = registry.acquire(lock)

        assertSame(first.loaderFor(shaA), second.loaderFor(shaA))
        assertEquals(1, registry.sharedLoaderCount())
        assertEquals(2, registry.references(first.moduleKeys().single()))

        first.close()
        assertEquals(1, registry.references(second.moduleKeys().single()))
        second.close()
        assertEquals(0, registry.sharedLoaderCount())
    }

    @Test
    fun `keeps different versions isolated`() {
        setUp()
        val jarA2 = jarOf(FixtureA::class, marker = "v2")
        val shaA2 = FileSystemPluginModuleStore.sha256(jarA2)
        store.install(shaA2, jarA2)

        val first = registry.acquire(lockOf(LockedModule("fixture", "a", "1.0.0", shaA)))
        val second = registry.acquire(lockOf(LockedModule("fixture", "a", "1.0.1", shaA2)))

        val loader1 = first.loaderFor(shaA)!!
        val loader2 = second.loaderFor(shaA2)!!
        assertNotSame(loader1, loader2)
        assertNotSame(
            loader1.loadClass("fixture.a.FixtureA"),
            loader2.loadClass("fixture.a.FixtureA")
        )

        first.close()
        second.close()
    }

    @Test
    fun `resolves plugin classes through the module graph`() {
        setUp()
        val lock = lockOf(
            LockedModule("fixture", "a", "1.0.0", shaA),
            LockedModule("fixture", "b", "1.0.0", shaB, dependencies = listOf(shaA))
        )
        val graph = registry.acquire(lock)
        val pluginJar = root.resolve("plugin.jar")
        Files.write(pluginJar, jarOf(FixturePlugin::class))

        PluginClassLoader(pluginJar, platform, graph.loaders().toList()).use { loader ->
            val type = loader.loadClass("fixture.plugin.FixturePlugin")
            val instance = type.getDeclaredConstructor().newInstance()
            assertEquals("A1B1P", type.getMethod("value").invoke(instance))
        }

        graph.close()
    }

    @Test
    fun `reports missing modules`() {
        setUp()
        val missing = LockedModule("fixture", "missing", "1.0.0", "c".repeat(64))
        assertFailsWith<PluginModuleMissingException> {
            registry.acquire(lockOf(missing))
        }
    }

    private fun lockOf(vararg modules: LockedModule): PluginModuleLock =
        PluginModuleLock(modules = modules.toList())
}
