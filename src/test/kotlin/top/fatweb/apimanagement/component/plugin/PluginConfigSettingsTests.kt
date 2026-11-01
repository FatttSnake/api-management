package top.fatweb.apimanagement.component.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin settings view and plugin-owned key tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginConfigSchemaUtil.visibleValues
 */
class PluginConfigSettingsTests {
    private val schema =
        PluginConfigSchemaUtil.parse(
            """
            {
              "groups": [
                {
                  "key": "share",
                  "fields": [
                    { "key": "ttlHours", "type": "number", "default": 2, "integer": true },
                    { "key": "enabled", "type": "boolean", "default": true },
                    { "key": "mode", "type": "enum", "default": "fast",
                      "options": [ { "value": "fast" }, { "value": "slow" } ] },
                    { "key": "notice", "type": "text" },
                    { "key": "token", "type": "secret" }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )!!

    @Test
    fun `settings all includes declared defaults and excludes secrets`() {
        val values =
            PluginConfigSchemaUtil.visibleValues(
                schema,
                mapOf("ttlHours" to "5", "mode" to "slow", "token" to "cipher", "startedAt" to "1"),
            )

        assertEquals("5", values["ttlHours"], "a stored value wins over the declared default")
        assertEquals("true", values["enabled"], "a declared key that was never stored reads its default")
        assertEquals("slow", values["mode"])
        assertEquals("1", values["startedAt"], "a plugin-owned key is included")
        assertFalse(values.containsKey("notice"), "a declared key with neither value nor default is absent")
        assertFalse(values.containsKey("token"), "a secret is never included")
    }

    @Test
    fun `settings all keeps an empty stored value`() {
        assertEquals("", PluginConfigSchemaUtil.visibleValues(schema, mapOf("notice" to ""))["notice"])
    }

    @Test
    fun `settings all without a schema returns what is stored`() {
        assertEquals(
            mapOf("startedAt" to "1"),
            PluginConfigSchemaUtil.visibleValues(null, mapOf("startedAt" to "1")),
        )
    }

    @Test
    fun `changed keys are the effective differences`() {
        val stored = mapOf("ttlHours" to "5", "mode" to "slow", "token" to "s3cr3t")
        val previous = PluginConfigSchemaUtil.effectiveValues(schema, emptyMap(), stored)
        val effective =
            PluginConfigSchemaUtil.effectiveValues(
                schema,
                mapOf("share" to mapOf("ttlHours" to "", "enabled" to "false", "notice" to "")),
                stored,
            )

        assertEquals(
            setOf("ttlHours", "enabled", "notice"),
            PluginConfigSchemaUtil.changedKeys(previous, effective),
        )
    }

    @Test
    fun `clearing a key already at its default is not a change`() {
        val previous = PluginConfigSchemaUtil.effectiveValues(schema, emptyMap(), emptyMap())
        val effective =
            PluginConfigSchemaUtil.effectiveValues(
                schema,
                mapOf("share" to mapOf("ttlHours" to "", "token" to "")),
                emptyMap(),
            )

        assertTrue(PluginConfigSchemaUtil.changedKeys(previous, effective).isEmpty())
    }

    @Test
    fun `plugin writes are refused for declared keys`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.requirePluginOwned(schema, "ttlHours")
        }
        PluginConfigSchemaUtil.requirePluginOwned(schema, "startedAt")
        PluginConfigSchemaUtil.requirePluginOwned(null, "startedAt")
    }
}
