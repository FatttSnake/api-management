package top.fatweb.apimanagement.sdk.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin library index tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginLibraryIndex
 */
class PluginLibraryIndexTests {
    private companion object {
        const val A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }

    @Test
    fun `an index survives a round trip`() {
        val index = PluginLibraryIndex(
            libraries = listOf(
                PluginLibraryEntry(A, "a.jar", listOf("g:a:1"), listOf(B)),
                PluginLibraryEntry(B, "b.jar", listOf("g:b:1"))
            )
        )

        val parsed = PluginLibraryIndex.read(PluginLibraryIndex.write(index))

        assertEquals(index, parsed)
    }

    @Test
    fun `a malformed digest is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLibraryIndex.read("""{"format":1,"libraries":[{"sha256":"nope","file":"a.jar"}]}""")
        }
    }

    @Test
    fun `a missing dependency is rejected`() {
        val index = PluginLibraryIndex(
            libraries = listOf(PluginLibraryEntry(A, "a.jar", dependsOn = listOf(B)))
        )
        assertFailsWith<IllegalArgumentException> { PluginLibraryIndex.write(index) }
    }

    @Test
    fun `a dependency cycle is rejected`() {
        val index = PluginLibraryIndex(
            libraries = listOf(
                PluginLibraryEntry(A, "a.jar", dependsOn = listOf(B)),
                PluginLibraryEntry(B, "b.jar", dependsOn = listOf(A))
            )
        )
        assertFailsWith<IllegalArgumentException> { PluginLibraryIndex.write(index) }
    }

    @Test
    fun `bouncycastle is detected from module coordinates`() {
        val bundled = PluginLibraryIndex(
            libraries = listOf(PluginLibraryEntry(A, "a.jar", listOf("org.bouncycastle:bcprov-jdk18on:1.86")))
        )
        val gateway = PluginLibraryIndex(
            libraries = listOf(PluginLibraryEntry(A, "a.jar", listOf("org.apache.commons:commons-lang3:3.20.0")))
        )

        assertTrue(PluginLibraryIndex.bundlesBouncyCastle(bundled))
        assertFalse(PluginLibraryIndex.bundlesBouncyCastle(gateway))
    }
}
