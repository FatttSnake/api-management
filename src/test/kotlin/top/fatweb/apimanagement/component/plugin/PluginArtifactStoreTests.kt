package top.fatweb.apimanagement.component.plugin

import org.mockito.Mockito.mock
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Plugin artifact store tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginArtifactStore
 */
class PluginArtifactStoreTests {
    private val store =
        PluginArtifactStore(
            ServerProperties(),
            mock(IApiPluginTrustKeyService::class.java),
        )

    @Test
    fun `reads a dependency lock from jar bytes`() {
        val lock = store.lock(jar("META-INF/api-plugin-deps.json" to LOCK))

        assertEquals(1, lock.modules.size)
        assertEquals("com.example:demo:1.0.0", lock.modules.single().coordinate)
    }

    @Test
    fun `a jar without a lock has no modules`() {
        val lock = store.lock(jar("META-INF/MANIFEST.MF" to "Manifest-Version: 1.0\n"))

        assertTrue(lock.modules.isEmpty())
    }

    @Test
    fun `an invalid lock is rejected`() {
        assertFailsWith<PluginInstallException> {
            store.lock(jar("META-INF/api-plugin-deps.json" to "not json"))
        }
    }

    private fun jar(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private companion object {
        val LOCK = """
            {"format":1,"constraints":{},"modules":[{"group":"com.example","name":"demo",
            "version":"1.0.0","sha256":"${"a".repeat(64)}","size":12,"dependencies":[]}]}
        """.trimIndent()
    }
}
