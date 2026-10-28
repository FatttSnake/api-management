package top.fatweb.apimanagement.plugin.runtime.module

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class FileSystemPluginModuleStoreTests {
    @TempDir
    lateinit var root: Path

    @Test
    fun `installs, verifies and reuses a module`() {
        val store = FileSystemPluginModuleStore(root)
        val bytes = "module-a".toByteArray()
        val digest = FileSystemPluginModuleStore.sha256(bytes)

        val path = store.install(digest, bytes)
        assertEquals("$digest.jar", path.fileName.toString())
        assertTrue(store.contains(digest))
        assertTrue(store.verify(digest))
        assertEquals(listOf(digest), store.list())

        assertEquals(path, store.install(digest, bytes))
    }

    @Test
    fun `rejects a digest mismatch`() {
        val store = FileSystemPluginModuleStore(root)
        assertFailsWith<PluginModuleStoreException> {
            store.install("0".repeat(64), "module-a".toByteArray())
        }
    }

    @Test
    fun `rejects a module over the size limit`() {
        val store = FileSystemPluginModuleStore(root, maxModuleBytes = 4)
        val bytes = "too-large".toByteArray()
        assertFailsWith<PluginModuleStoreException> {
            store.install(FileSystemPluginModuleStore.sha256(bytes), bytes)
        }
    }

    @Test
    fun `collects unreferenced modules`() {
        val store = FileSystemPluginModuleStore(root)
        val referenced = "referenced".toByteArray()
        val unused = "unused".toByteArray()
        val referencedDigest = FileSystemPluginModuleStore.sha256(referenced)
        val unusedDigest = FileSystemPluginModuleStore.sha256(unused)
        store.install(referencedDigest, referenced)
        store.install(unusedDigest, unused)

        assertEquals(1, store.gc(setOf(referencedDigest)))
        assertTrue(store.contains(referencedDigest))
        assertFalse(store.contains(unusedDigest))
    }

    @Test
    fun `rejects an invalid digest`() {
        val store = FileSystemPluginModuleStore(root)
        assertFailsWith<PluginModuleStoreException> {
            store.path("../escape")
        }
    }
}
