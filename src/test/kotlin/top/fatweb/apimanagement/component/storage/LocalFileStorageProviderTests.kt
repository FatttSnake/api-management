package top.fatweb.apimanagement.component.storage

import org.junit.jupiter.api.io.TempDir
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.properties.StorageProperties
import top.fatweb.apimanagement.util.sha256HexString
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Local file storage provider tests
 *
 * A plugin reaches this provider in process, without going through the servlet
 * container, so the checks that keep a key inside the storage root have to hold
 * here rather than depending on the container rejecting a malformed URL.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see LocalFileStorageProvider
 */
class LocalFileStorageProviderTests {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

    private fun providerAt(root: Path, baseUrl: String? = null): LocalFileStorageProvider {
        val properties = ServerProperties(
            storage = StorageProperties(
                local = StorageProperties.LocalStorageProperties(root = root.toString()),
                publicBaseUrl = baseUrl
            )
        )

        return LocalFileStorageProvider(properties, ExternalLinkSigner(properties), PublicBaseUrlResolver(properties))
    }

    @Test
    fun `saveAt stores raw bytes under the documented layout`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)

        provider.saveAt("plugin-data/alpha/avatar/1.png", png)

        val stored = tempDir.resolve("files/plugin-data/alpha/avatar/1.png")
        assertTrue(stored.exists(), "expected the file below {root}/files/plugin-data/{pluginId}/")
        // Raw, not zstd - this is what makes an external URL possible at all.
        assertContentEquals(png, stored.readBytes())
        // The pre-{root}/files layout must not come back.
        assertFalse(tempDir.resolve("plugin-data").exists())
    }

    @Test
    fun `saveAt and loadAt round trip`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)
        provider.saveAt("plugin-data/alpha/avatar/1.png", png)

        assertContentEquals(png, provider.loadAt("plugin-data/alpha/avatar/1.png"))
        assertTrue(provider.existsAt("plugin-data/alpha/avatar/1.png"))
        assertNull(provider.loadAt("plugin-data/alpha/avatar/missing.png"))
        assertFalse(provider.existsAt("plugin-data/alpha/avatar/missing.png"))
    }

    @Test
    fun `a location key is confined to the files root`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)
        val key = StorageKeyUtil.pluginKey("alpha", "avatar/1.png")

        provider.saveAt(key, png)

        assertEquals("plugin-data/alpha/avatar/1.png", key)
        assertTrue(tempDir.resolve("files/$key").exists())
        assertTrue(tempDir.resolve("files").isDirectory())
    }

    @Test
    fun `every escaping key is rejected before anything is written`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)
        val outside = tempDir.resolve("sibling").createDirectories()

        listOf(
            "plugin-data/alpha/../../../../escape.txt",
            "plugin-data/alpha/..\\..\\..\\escape.txt",
            "plugin-data/alpha//etc/passwd",
            "plugin-data/alpha/C:\\Windows\\win.ini",
            "../alpha/escape.txt",
            "/etc/passwd"
        ).forEach { key ->
            assertFailsWith<IllegalArgumentException>("expected '$key' to be rejected") {
                provider.saveAt(key, png)
            }
        }

        assertFalse(outside.resolve("escape.txt").exists())
        assertFalse(Path("escape.txt").exists())
        assertFalse(tempDir.resolve("escape.txt").exists())
    }

    @Test
    fun `deleteAt reports whether anything was removed`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)
        provider.saveAt("plugin-data/alpha/avatar/1.png", png)

        assertTrue(provider.deleteAt("plugin-data/alpha/avatar/1.png"))
        assertFalse(provider.deleteAt("plugin-data/alpha/avatar/1.png"))
        assertFalse(provider.existsAt("plugin-data/alpha/avatar/1.png"))
    }

    @Test
    fun `deleteAtPrefix removes the namespace and nothing else`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)
        provider.saveAt(StorageKeyUtil.pluginKey("alpha", "avatar/1.png"), png)
        provider.saveAt(StorageKeyUtil.pluginKey("alpha", "avatar/2.png"), png)
        provider.saveAt(StorageKeyUtil.pluginKey("beta", "avatar/1.png"), png)

        assertEquals(2, provider.deleteAtPrefix(StorageKeyUtil.pluginBaseKey("alpha")))

        assertFalse(tempDir.resolve("files/plugin-data/alpha").exists())
        assertTrue(tempDir.resolve("files/plugin-data/beta/avatar/1.png").exists())
        // Idempotent - a retried uninstall must not fail.
        assertEquals(0, provider.deleteAtPrefix(StorageKeyUtil.pluginBaseKey("alpha")))
    }

    @Test
    fun `externalUrlAt signs a link the gateway can verify`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir, baseUrl = "https://cdn.example.test/")
        val verifier = ExternalLinkSigner(ServerProperties())

        val url = assertNotNull(
            provider.externalUrlAt(
                StorageKeyUtil.pluginKey("alpha", "avatar/1.png"),
                StorageKeyUtil.pluginReference("alpha", "avatar/1.png"),
                Duration.ofHours(1)
            )
        )

        // The reference, not the storage key, and exactly one slash at the join despite
        // the trailing slash in the configured base URL.
        assertTrue(url.startsWith("https://cdn.example.test/public/storage/alpha/avatar/1.png?e="), url)
        assertFalse(url.contains("plugin-data"))

        val expiresAt = Regex("e=(\\d+)").find(url)!!.groupValues[1].toLong()
        val signature = Regex("s=([\\w-]+)").find(url)!!.groupValues[1]
        assertTrue(verifier.verify("alpha/avatar/1.png", expiresAt, signature))
        assertFalse(verifier.verify("alpha/avatar/2.png", expiresAt, signature))
        // The storage key must not be accepted as the signed reference either.
        assertFalse(verifier.verify("plugin-data/alpha/avatar/1.png", expiresAt, signature))
    }

    @Test
    fun `externalUrlAt rejects an escaping reference`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir, baseUrl = "https://cdn.example.test")

        listOf("../escape.txt", "/etc/passwd", "a//b").forEach { reference ->
            assertFailsWith<IllegalArgumentException>("expected '$reference' to be rejected") {
                provider.externalUrlAt("plugin-data/alpha/x.png", reference, Duration.ofHours(1))
            }
        }
    }

    @Test
    fun `externalUrlAt returns null when no base can be resolved`(@TempDir tempDir: Path) {
        // No request context and no configured base URL: a background thread cannot
        // invent an absolute URL, so the caller is told so rather than handed a
        // relative path that looks usable.
        assertNull(
            providerAt(tempDir).externalUrlAt(
                "plugin-data/alpha/avatar/1.png",
                "alpha/avatar/1.png",
                Duration.ofHours(1)
            )
        )
    }

    @Test
    fun `content addressing stays compressed under objects`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)

        val key = provider.save(png)

        assertEquals(png.sha256HexString(), key)
        val stored = tempDir.resolve("objects/${key.substring(0, 2)}/${key.substring(2)}")
        assertTrue(stored.exists())
        assertContentEquals(byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte()), stored.readBytes().copyOf(4))
        assertContentEquals(png, provider.load(key))
    }

    @Test
    fun `content keys are validated before a path is resolved`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)

        // '..' used to shift the resolved path out of objects/, and a short key threw
        // StringIndexOutOfBoundsException while splitting the shard directory.
        listOf("../..", "..", "ab", "", "Z".repeat(64)).forEach { key ->
            assertFailsWith<IllegalArgumentException>("expected '$key' to be rejected") { provider.load(key) }
        }
    }

    @Test
    fun `a saved file can be served back through the signed route`(@TempDir tempDir: Path) {
        // The end-to-end shape the controller relies on: mint, then rebuild both the
        // reference and the key out of the URL exactly the way the controller does.
        val provider = providerAt(tempDir, baseUrl = "http://localhost:8111")
        val verifier = ExternalLinkSigner(ServerProperties())
        provider.saveAt(StorageKeyUtil.pluginKey("alpha", "avatar/1.png"), png)

        val url = assertNotNull(
            provider.externalUrlAt(
                StorageKeyUtil.pluginKey("alpha", "avatar/1.png"),
                StorageKeyUtil.pluginReference("alpha", "avatar/1.png"),
                Duration.ofHours(1)
            )
        )

        val reference = url.substringAfter("/public/storage/").substringBefore('?')
        val expiresAt = Regex("e=(\\d+)").find(url)!!.groupValues[1].toLong()
        val signature = Regex("s=([\\w-]+)").find(url)!!.groupValues[1]

        assertEquals("alpha/avatar/1.png", reference)
        assertTrue(verifier.verify(reference, expiresAt, signature))
        assertContentEquals(png, provider.loadAt(StorageKeyUtil.pluginKey("alpha", "avatar/1.png")))
    }

    @Test
    fun `saving the same content twice keeps one object`(@TempDir tempDir: Path) {
        val provider = providerAt(tempDir)

        val first = provider.save(png)
        val second = provider.save(png)

        assertEquals(first, second)
        assertEquals(1, Files.list(tempDir.resolve("objects")).use { it.count() })
    }
}
