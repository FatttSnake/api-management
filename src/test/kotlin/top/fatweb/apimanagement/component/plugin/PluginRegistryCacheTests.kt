package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Plugin registry cache tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginRegistryCache
 */
class PluginRegistryCacheTests {
    private val cache = PluginRegistryCache()

    private fun interfaceOf(code: String, pluginId: String) = ApiInterface().apply {
        this.code = code
        this.pluginId = pluginId
    }

    private fun pluginOf(pluginId: String) = ApiPlugin().apply { this.pluginId = pluginId }

    @Test
    fun `an empty cache resolves nothing`() {
        assertNull(cache.getByCode("api:echo:v1:ping"))
        assertNull(cache.getByPluginId("echo"))
    }

    @Test
    fun `replaceAll publishes both maps`() {
        val apiInterface = interfaceOf("api:echo:v1:ping", "echo")
        val plugin = pluginOf("echo")

        cache.replaceAll(mapOf("api:echo:v1:ping" to apiInterface), mapOf("echo" to plugin))

        assertSame(apiInterface, cache.getByCode("api:echo:v1:ping"))
        assertSame(plugin, cache.getByPluginId("echo"))
    }

    @Test
    fun `a replaced snapshot drops what it no longer holds`() {
        cache.replaceAll(
            mapOf("api:echo:v1:ping" to interfaceOf("api:echo:v1:ping", "echo")),
            mapOf("echo" to pluginOf("echo")),
        )

        cache.replaceAll(emptyMap(), emptyMap())

        assertNull(cache.getByCode("api:echo:v1:ping"))
        assertNull(cache.getByPluginId("echo"))
    }

    @Test
    fun `a reader never sees an emptied cache`() {
        val code = "api:echo:v1:ping"
        // Two snapshots that both carry the code, and nothing else in common: whatever a reader
        // interleaves with, the code has to resolve - and to a plugin that is in the same
        // snapshot it read the interface from
        val first = mapOf(code to interfaceOf(code, "echo")) to mapOf("echo" to pluginOf("echo"))
        val second =
            mapOf(code to interfaceOf(code, "echo")) to
                mapOf("echo" to pluginOf("echo"), "other" to pluginOf("other"))
        val snapshots = listOf(first, second)

        val start = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val iterations = 20_000

        // Published before anyone reads: what is under test is that a replacement is never
        // visible as an absence, not what an empty cache answers
        cache.replaceAll(first.first, first.second)

        val writer =
            thread {
                start.await()
                repeat(iterations) { index ->
                    val next = snapshots[index % snapshots.size]
                    cache.replaceAll(next.first, next.second)
                }
            }
        val readers =
            List(4) {
                thread {
                    start.await()
                    repeat(iterations) {
                        val resolved = cache.getByCode(code)
                        if (resolved == null) {
                            failure.compareAndSet(
                                null,
                                AssertionError("getByCode returned null for a code present in every snapshot"),
                            )
                            return@thread
                        }

                        if (cache.getByPluginId(resolved.pluginId!!) == null) {
                            failure.compareAndSet(
                                null,
                                AssertionError("An interface resolved to a plugin missing from its own snapshot"),
                            )
                            return@thread
                        }
                    }
                }
            }

        start.countDown()
        writer.join(TimeUnit.SECONDS.toMillis(30))
        readers.forEach { it.join(TimeUnit.SECONDS.toMillis(30)) }

        assertNotNull(cache.getByCode(code))
        failure.get()?.let { throw it }
    }
}
