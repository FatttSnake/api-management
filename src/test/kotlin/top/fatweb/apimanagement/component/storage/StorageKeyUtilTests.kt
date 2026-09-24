package top.fatweb.apimanagement.component.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Storage key util tests
 *
 * A path accepted here becomes a filesystem path below the storage root, so every
 * case that could leave that root has to be rejected rather than escaped.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see StorageKeyUtil
 */
class StorageKeyUtilTests {
    private val escapeAttempts = listOf(
        "../escape.txt",
        "..\\..\\escape.txt",
        "/etc/passwd",
        "C:\\Windows\\win.ini",
        "a//b",
        "a/",
        "/a",
        ".",
        "..",
        "a/./b",
        "a/../../b",
        "CON",
        "nul.txt",
        "com1.bin",
        "a".repeat(201),
        "avatar/in valid.png",
        "avatar/1.png "
    )

    @Test
    fun `pluginKey composes the plugin namespace under plugin-data`() {
        assertEquals(
            "plugin-data/alpha/avatar/1.png",
            StorageKeyUtil.pluginKey("alpha", "avatar/1.png")
        )
        assertEquals("plugin-data/alpha", StorageKeyUtil.pluginBaseKey("alpha"))
    }

    @Test
    fun `pluginReference is the plugin namespace without the storage layout`() {
        // The reference is what an external URL carries, so it deliberately does not
        // expose where the file actually lives.
        assertEquals("alpha/avatar/1.png", StorageKeyUtil.pluginReference("alpha", "avatar/1.png"))
    }

    @Test
    fun `pluginKey and pluginReference reject an invalid plugin id`() {
        listOf("Alpha", "../alpha", "alpha/beta", "", "-alpha").forEach { pluginId ->
            assertFailsWith<IllegalArgumentException>("expected '$pluginId' to be rejected") {
                StorageKeyUtil.pluginKey(pluginId, "1.png")
            }
            assertFailsWith<IllegalArgumentException>("expected '$pluginId' to be rejected") {
                StorageKeyUtil.pluginReference(pluginId, "1.png")
            }
            assertFailsWith<IllegalArgumentException>("expected '$pluginId' to be rejected") {
                StorageKeyUtil.pluginBaseKey(pluginId)
            }
        }
    }

    @Test
    fun `pluginKey rejects every escaping path`() {
        escapeAttempts.forEach { path ->
            assertFailsWith<IllegalArgumentException>("expected '$path' to be rejected") {
                StorageKeyUtil.pluginKey("alpha", path)
            }
            assertFailsWith<IllegalArgumentException>("expected '$path' to be rejected") {
                StorageKeyUtil.pluginReference("alpha", path)
            }
        }
    }

    @Test
    fun `every plugin key stays inside its own namespace`() {
        // The whole isolation guarantee: whatever the plugin asks for, the composed
        // key begins with its own prefix and the path can only append below it.
        listOf(
            "avatar/1.png",
            // A path that names another namespace is nested, not an escape - it lands
            // inside alpha's own prefix like any other path.
            "plugin-data/beta/avatar/1.png",
            "beta/avatar/1.png",
            "objects/alpha.png"
        ).forEach { path ->
            val key = StorageKeyUtil.pluginKey("alpha", path)

            assertTrue(key.startsWith("plugin-data/alpha/"), key)
            assertEquals("plugin-data/alpha/$path", key)
        }
    }

    @Test
    fun `validateLocationKey is generic and rejects traversal`() {
        // It validates a system-level relative path, so it has no opinion about who
        // owns the key - only about whether it could leave the location root.
        StorageKeyUtil.validateLocationKey("plugin-data/alpha/avatar/1.png")
        StorageKeyUtil.validateLocationKey("alpha/avatar/1.png")
        StorageKeyUtil.validateLocationKey("reports/2026/2026-09.csv")
        StorageKeyUtil.validateLocationKey("alpha")

        escapeAttempts.forEach { key ->
            assertFailsWith<IllegalArgumentException>("expected '$key' to be rejected") {
                StorageKeyUtil.validateLocationKey(key)
            }
        }
    }

    @Test
    fun `content keys are exactly one lowercase sha256 digest`() {
        assertTrue(StorageKeyUtil.CONTENT_KEY_REGEX.matches("a".repeat(64)))

        // A short key is the case that used to throw StringIndexOutOfBoundsException
        // while resolving the shard directory.
        listOf("", "ab", "..", "A".repeat(64), "a".repeat(63), "a".repeat(65))
            .forEach { assertFalse(StorageKeyUtil.CONTENT_KEY_REGEX.matches(it), "expected '$it' to be rejected") }
    }
}
