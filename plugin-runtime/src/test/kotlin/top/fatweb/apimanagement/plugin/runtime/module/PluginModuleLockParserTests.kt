package top.fatweb.apimanagement.plugin.runtime.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleLockParserTests {
    private val parser = PluginModuleLockParser()
    private val shaA = "a".repeat(64)
    private val shaB = "b".repeat(64)

    @Test
    fun `parses a valid lock`() {
        val lock = parser.parse(
            """
            {
              "format": 1,
              "constraints": { "fixture:a": "[1.0,2.0)" },
              "modules": [
                {
                  "group": "fixture",
                  "name": "a",
                  "version": "1.0.0",
                  "sha256": "$shaA",
                  "size": 10
                },
                {
                  "group": "fixture",
                  "name": "b",
                  "version": "1.0.0",
                  "sha256": "$shaB",
                  "size": 20,
                  "dependencies": ["$shaA"]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, lock.format)
        assertEquals("[1.0,2.0)", lock.constraints["fixture:a"])
        assertEquals(listOf("fixture:a:1.0.0", "fixture:b:1.0.0"), lock.modules.map { it.coordinate })
        assertEquals(listOf(shaA), lock.modules[1].dependencies)
    }

    @Test
    fun `rejects a missing dependency`() {
        assertFailsWith<PluginModuleLockException> {
            parser.parse(lockJson(module(name = "b", sha = shaB, dependencies = listOf("c".repeat(64)))))
        }
    }

    @Test
    fun `rejects a dependency cycle`() {
        val modules = """
            ${module(name = "a", sha = shaA, dependencies = listOf(shaB))},
            ${module(name = "b", sha = shaB, dependencies = listOf(shaA))}
        """.trimIndent()
        assertFailsWith<PluginModuleLockException> {
            parser.parse(lockJson(modules))
        }
    }

    @Test
    fun `rejects dynamic versions`() {
        assertFailsWith<PluginModuleLockException> {
            parser.parse(lockJson(module(name = "a", version = "1.0.0-SNAPSHOT", sha = shaA)))
        }
    }

    @Test
    fun `rejects an invalid digest`() {
        assertFailsWith<PluginModuleLockException> {
            parser.parse(lockJson(module(name = "a", sha = "not-a-digest")))
        }
    }

    @Test
    fun `rejects duplicate coordinates`() {
        val modules = """
            ${module(name = "a", sha = shaA)},
            ${module(name = "a", sha = shaB)}
        """.trimIndent()
        assertFailsWith<PluginModuleLockException> {
            parser.parse(lockJson(modules))
        }
    }

    @Test
    fun `ignores unknown fields`() {
        val lock = parser.parse(
            lockJson(
                """
                {
                  "group": "fixture",
                  "name": "a",
                  "version": "1.0.0",
                  "sha256": "$shaA",
                  "future": true
                }
                """.trimIndent()
            )
        )
        assertTrue(lock.modules.single().coordinate == "fixture:a:1.0.0")
    }

    private fun lockJson(modules: String): String =
        """{ "format": 1, "modules": [ $modules ] }"""

    private fun module(
        name: String,
        version: String = "1.0.0",
        sha: String = shaA,
        dependencies: List<String> = emptyList()
    ): String {
        val deps = dependencies.joinToString(",") { "\"$it\"" }
        return """
            {
              "group": "fixture",
              "name": "$name",
              "version": "$version",
              "sha256": "$sha",
              "size": 10,
              "dependencies": [$deps]
            }
        """.trimIndent()
    }
}
