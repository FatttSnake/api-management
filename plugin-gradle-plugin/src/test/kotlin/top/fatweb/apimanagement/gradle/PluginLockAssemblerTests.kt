package top.fatweb.apimanagement.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginLockAssemblerTests {
    private val shaA = "a".repeat(64)
    private val shaB = "b".repeat(64)
    private val shaC = "c".repeat(64)
    private val a = LockArtifact("fixture", "a", "1.0.0", shaA, 10)
    private val b = LockArtifact("fixture", "b", "1.0.0", shaB, 20)
    private val c = LockArtifact("fixture", "c", "1.0.0", shaC, 30)

    @Test
    fun `writes format 1 and maps shared loader coordinates to digests`() {
        val document =
            PluginLockAssembler.assemble(
                artifacts = listOf(a, b),
                edges = listOf("fixture:b:1.0.0=fixture:a:1.0.0"),
                sharedLoaders = listOf(listOf("fixture:b", "fixture:a")),
            )

        assertEquals(1, document.format)
        assertEquals(listOf(listOf(shaA, shaB)), document.groups)
        assertEquals(listOf("fixture:a:1.0.0", "fixture:b:1.0.0"), document.modules.map { it.coordinate })
        assertEquals(listOf(shaA), document.modules[1].dependencies)
        assertEquals(mapOf("fixture:a" to "1.0.0", "fixture:b" to "1.0.0"), document.constraints)
    }

    @Test
    fun `output does not depend on input order`() {
        val first =
            PluginLockAssembler.assemble(
                artifacts = listOf(a, b, c),
                edges = listOf("fixture:b:1.0.0=fixture:a:1.0.0", "fixture:c:1.0.0=fixture:a:1.0.0"),
                sharedLoaders = listOf(listOf("fixture:a", "fixture:b")),
            )
        val second =
            PluginLockAssembler.assemble(
                artifacts = listOf(c, b, a),
                edges = listOf("fixture:c:1.0.0=fixture:a:1.0.0", "fixture:b:1.0.0=fixture:a:1.0.0"),
                sharedLoaders = listOf(listOf("fixture:b", "fixture:a")),
            )

        assertEquals(PluginLockAssembler.toJson(first), PluginLockAssembler.toJson(second))
    }

    @Test
    fun `writes no groups when none are configured`() {
        val document =
            PluginLockAssembler.assemble(
                artifacts = listOf(a, b),
                edges = listOf("fixture:b:1.0.0=fixture:a:1.0.0"),
                sharedLoaders = emptyList(),
            )

        assertEquals(emptyList(), document.groups)
        assertEquals(listOf(shaA), document.modules[1].dependencies)
    }

    @Test
    fun `rejects a group with fewer than two modules`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(listOf(a, b), emptyList(), listOf(listOf("fixture:a")))
        }
    }

    @Test
    fun `rejects an unknown module`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(listOf(a, b), emptyList(), listOf(listOf("fixture:a", "fixture:missing")))
        }
    }

    @Test
    fun `rejects a module provided by the gateway`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(
                listOf(a, b),
                emptyList(),
                listOf(listOf("fixture:a", "fixture:b")),
                gatewayProvided = setOf("fixture:b"),
            )
        }
    }

    @Test
    fun `rejects a duplicate module in one group`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(listOf(a, b), emptyList(), listOf(listOf("fixture:a", "fixture:a")))
        }
    }

    @Test
    fun `rejects a module in two groups`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(
                listOf(a, b, c),
                emptyList(),
                listOf(listOf("fixture:a", "fixture:b"), listOf("fixture:b", "fixture:c")),
            )
        }
    }

    @Test
    fun `rejects a coordinate with a version`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(
                listOf(a, b),
                emptyList(),
                listOf(listOf("fixture:a:1.0.0", "fixture:b")),
            )
        }
    }

    @Test
    fun `rejects groups that create a loader unit cycle`() {
        assertFailsWith<IllegalArgumentException> {
            PluginLockAssembler.assemble(
                artifacts = listOf(a, b, c),
                edges = listOf("fixture:a:1.0.0=fixture:b:1.0.0", "fixture:b:1.0.0=fixture:c:1.0.0"),
                sharedLoaders = listOf(listOf("fixture:a", "fixture:c")),
            )
        }
    }
}
