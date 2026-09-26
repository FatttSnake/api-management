package top.fatweb.apimanagement.component.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Plugin config schema util tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginConfigSchemaUtil
 */
class PluginConfigSchemaUtilTests {
    private val schema = PluginConfigSchemaUtil.parse(
        """
        {
          "version": 1,
          "datasources": [ { "name": "cache", "dbType": "sqlite", "required": true } ],
          "groups": [
            {
              "key": "share",
              "title": "分享设置",
              "fields": [
                { "key": "ttlHours", "type": "number", "title": "有效期", "default": 2,
                  "minimum": 1, "maximum": 168, "integer": true },
                { "key": "enabled", "type": "boolean", "default": true },
                { "key": "mode", "type": "enum", "default": "fast",
                  "options": [ { "value": "fast", "label": "快速" }, { "value": "slow" } ] },
                { "key": "notice", "type": "text", "maxLength": 5 },
                { "key": "token", "type": "secret", "required": true }
              ]
            }
          ]
        }
        """.trimIndent()
    )!!

    @Test
    fun `a declared schema is readable`() {
        assertEquals(1, schema.version)
        assertEquals("cache", schema.datasources.single().name)
        assertEquals(true, schema.datasources.single().required)
        assertEquals(PluginDatasourceType.SQLITE, schema.datasources.single().dbType)
        assertEquals("share", schema.groups.single().key)
        assertEquals(5, schema.fields.size)
        assertTrue(schema.isDeclared("notice"))
        assertFalse(schema.isDeclared("installedAt"))
        assertEquals("cache", schema.datasourceOf("cache")?.name)
        assertNull(schema.datasourceOf("main"))
    }

    @Test
    fun `an absent schema is not an error`() {
        assertNull(PluginConfigSchemaUtil.parse(null))
        assertNull(PluginConfigSchemaUtil.parse(""))
        assertNull(PluginConfigSchemaUtil.parse("   "))
    }

    @Test
    fun `a declared number default is normalised to the stored form`() {
        // JSON 2 parses as an integer, and an integral default must survive the field's
        // own whole-number check rather than being rejected for being written as 2.0
        assertEquals("2", schema.defaultOf("ttlHours"))
        assertEquals("true", schema.defaultOf("enabled"))
    }

    @Test
    fun `a declared secret is recognised`() {
        assertTrue(schema.isSecret("token"))
        assertFalse(schema.isSecret("notice"))
        assertEquals(setOf("token"), schema.secretKeys())
    }

    @Test
    fun `a schema that is not a JSON object is rejected`() {
        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.parse("[1, 2, 3]") }
        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.parse("{ not json") }
    }

    @Test
    fun `a field without a type or with an unknown one is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseField("""{ "key": "a" }""") }
        assertFailsWith<IllegalArgumentException> { parseField("""{ "key": "a", "type": "colour" }""") }
    }

    @Test
    fun `a duplicated key is rejected`() {
        // A duplicate would leave two declarations fighting over one stored value
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "string" }, { "key": "a", "type": "string" }""")
        }
    }

    @Test
    fun `a secret default is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseField("""{ "key": "a", "type": "secret", "default": "x" }""") }
    }

    @Test
    fun `a constraint the type cannot honour is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "string", "maximum": 5 }""")
        }
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "number", "maxLength": 5 }""")
        }
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "string", "options": [ { "value": "x" } ] }""")
        }
    }

    @Test
    fun `a default that violates its own constraint is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "number", "default": 200, "maximum": 168 }""")
        }
    }

    @Test
    fun `an unparsable pattern is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            parseField("""{ "key": "a", "type": "string", "pattern": "[" }""")
        }
    }

    @Test
    fun `valid values pass`() {
        PluginConfigSchemaUtil.validate(
            schema,
            mapOf("ttlHours" to "3", "enabled" to "false", "mode" to "slow", "notice" to "hello", "token" to "s3cr3t")
        )
    }

    @Test
    fun `a value for an undeclared key is rejected`() {
        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.validate(schema, mapOf("nope" to "1")) }
    }

    @Test
    fun `a value of the wrong type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("enabled" to "yes", "token" to "x"))
        }
    }

    @Test
    fun `a number outside its range or with a fraction is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("ttlHours" to "169", "token" to "x"))
        }
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("ttlHours" to "1.5", "token" to "x"))
        }
    }

    @Test
    fun `an enum value outside its options is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("mode" to "medium", "token" to "x"))
        }
    }

    @Test
    fun `a value longer than its maximum is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("notice" to "too long", "token" to "x"))
        }
    }

    @Test
    fun `a required field is satisfied by a declared default or a stored value`() {
        // ttlHours is not required, but the secret is: it can only be satisfied by a
        // submission or by a value that is already stored, since it has no default
        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.validate(schema, emptyMap()) }

        PluginConfigSchemaUtil.validate(schema, emptyMap(), setOf("token"))
    }

    @Test
    fun `the mask of a stored secret is recognised`() {
        assertTrue(PluginConfigSchemaUtil.isMasked(PluginConfigSchemaUtil.SECRET_MASK))
        assertFalse(PluginConfigSchemaUtil.isMasked("s3cr3t"))
    }

    @Test
    fun `a masked secret means keep the stored one`() {
        // The mask is what the console sends back for a value it was never shown, so it
        // stands for the stored one rather than being a submitted value
        PluginConfigSchemaUtil.validate(schema, mapOf("token" to PluginConfigSchemaUtil.SECRET_MASK), setOf("token"))
    }

    @Test
    fun `a masked secret is not checked against the field constraints`() {
        // The mask is six characters, so a secret declaring a longer minimum would be
        // rejected by its own mask if the mask went through the value checks
        val constrained = parseField("""{ "key": "token", "type": "secret", "minLength": 20 }""")!!

        PluginConfigSchemaUtil.validate(constrained, mapOf("token" to PluginConfigSchemaUtil.SECRET_MASK))
    }

    @Test
    fun `a blank secret is not checked against the field constraints`() {
        // Clearing a secret leaves no value behind, so a length or pattern the field declares
        // has nothing to apply to
        val constrained = parseField("""{ "key": "token", "type": "secret", "minLength": 20 }""")!!

        PluginConfigSchemaUtil.validate(constrained, mapOf("token" to ""))
    }

    @Test
    fun `a masked secret without a stored value does not satisfy a required field`() {
        // Nothing is there to keep, so the required secret is still unsatisfied
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("token" to PluginConfigSchemaUtil.SECRET_MASK))
        }
    }

    @Test
    fun `a blank submission clears rather than satisfies a required field`() {
        // A blank value deletes the stored one, so it cannot count as the required value
        // just because something is still stored under that key
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, mapOf("token" to ""), setOf("token"))
        }
    }

    private fun parseField(fields: String) =
        PluginConfigSchemaUtil.parse("""{ "groups": [ { "key": "g", "fields": [ $fields ] } ] }""")

    @Test
    fun `a schema without a datasource declares none`() {
        val plain = PluginConfigSchemaUtil.parse("""{ "groups": [] }""")

        assertNotNull(plain)
        assertTrue(plain.datasources.isEmpty())
        assertTrue(plain.datasourceKeys.isEmpty())
    }

    @Test
    fun `an empty datasource array declares none`() {
        assertTrue(parseDatasources("[]")!!.datasources.isEmpty())
    }

    @Test
    fun `the replaced singular spelling is refused by name`() {
        // An unknown top-level key is ignored, so without this an old plugin would mount with
        // no datasource, no error, and its data silently out of reach
        val error = assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.parse("""{ "datasource": { "dbType": "sqlite" } }""")
        }

        assertTrue(
            error.message!!.contains("datasources"),
            "the refusal should name the replacement: ${error.message}"
        )
    }

    @Test
    fun `the declared dialect is required`() {
        // The administrator is never asked to choose one, so a declaration that leaves it
        // out has no way to be completed later: it is an authoring mistake, not a default
        assertFailsWith<IllegalArgumentException> { parseDatasources("""[ { "name": "main" } ]""") }
        assertFailsWith<IllegalArgumentException> { parseDatasources("""[ { "name": "main", "dbType": "" } ]""") }
    }

    @Test
    fun `an unknown dialect is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "postgres" } ]""")
        }

        assertTrue(error.message!!.contains("postgres"), "the refusal should name the offending value: ${error.message}")
    }

    @Test
    fun `a declared dialect is normalised`() {
        assertEquals(
            PluginDatasourceType.MYSQL,
            parseDatasources(mysqlDatasource)!!.datasources.single().dbType
        )
        assertEquals(
            PluginDatasourceType.SQLITE,
            parseDatasources("""[ { "name": "cache", "dbType": "SQLite" } ]""")!!.datasources.single().dbType
        )
    }

    @Test
    fun `a datasource may be optional or required`() {
        assertEquals(false, parseDatasources("""[ { "name": "cache", "dbType": "SQLITE" } ]""")!!.datasources.single().required)
        assertEquals(
            true,
            parseDatasources("""[ { "name": "cache", "dbType": "SQLITE", "required": true } ]""")!!.datasources.single().required
        )
    }

    @Test
    fun `the keys a datasource is composed from are the ones its slots name`() {
        assertEquals(
            setOf("db.host", "db.port", "db.name", "db.user", "db.password", "db.params"),
            parseDatasources(mysqlDatasource)!!.datasourceKeys
        )
    }

    @Test
    fun `a slot has to point at a declared field`() {
        // A slot that named nothing would be a connection fact the gateway can never read,
        // and the plugin author would see the plugin work locally and fail on install
        val error = assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "host": "nope", "database": "db.name" } ]""")
        }

        assertTrue(error.message!!.contains("nope"), "the refusal should name the key: ${error.message}")
    }

    @Test
    fun `a slot has to point at a field of the right type`() {
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "host": "db.port", "database": "db.name" } ]""")
        }
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "host": "db.host", "database": "db.name", "port": "db.user" } ]""")
        }
        // A password held as a plain field would be stored in the clear
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "host": "db.host", "database": "db.name", "password": "db.user" } ]""")
        }
    }

    @Test
    fun `a mysql datasource has to say where to connect to`() {
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "database": "db.name" } ]""")
        }
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "main", "dbType": "MYSQL", "host": "db.host" } ]""")
        }
    }

    @Test
    fun `a sqlite datasource declares no slot`() {
        // Nothing about it is the administrator's to decide, so a slot would promise a
        // configuration that never has anywhere to appear
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "cache", "dbType": "SQLITE", "host": "db.host" } ]""")
        }
    }

    @Test
    fun `one config key cannot describe two slots or two datasources`() {
        // Both would mean an administrator edits one field while a second reader they cannot
        // see changes with it
        assertFailsWith<IllegalArgumentException> {
            parseDatasources(
                """[ { "name": "main", "dbType": "MYSQL", "host": "db.host", "database": "db.host" } ]"""
            )
        }
        assertFailsWith<IllegalArgumentException> {
            parseDatasources(
                """[ { "name": "main", "dbType": "MYSQL", "host": "db.host", "database": "db.name" },
                    { "name": "other", "dbType": "MYSQL", "host": "db.host", "database": "db.name" } ]"""
            )
        }
    }

    @Test
    fun `a password slot cannot point at a required field`() {
        // A required field refuses a blank submission, so the stored password could never be
        // cleared - which is the whole reason the password is an ordinary secret field
        val error = assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.parse(
                """
                {
                  "datasources": [ { "name": "main", "dbType": "MYSQL", "host": "db.host", "database": "db.name",
                                     "password": "db.password" } ],
                  "groups": [
                    { "key": "db", "fields": [
                      { "key": "db.host", "type": "string" },
                      { "key": "db.name", "type": "string" },
                      { "key": "db.password", "type": "secret", "required": true }
                    ] }
                  ]
                }
                """.trimIndent()
            )
        }

        assertTrue(
            error.message!!.contains("cleared"),
            "the refusal should say why: ${error.message}"
        )
    }

    @Test
    fun `a duplicate datasource name is rejected`() {
        // The name is the handle the plugin asks for a datasource by, so two of them would
        // make that question unanswerable
        assertFailsWith<IllegalArgumentException> {
            parseDatasources(
                """[ { "name": "cache", "dbType": "SQLITE" }, { "name": "cache", "dbType": "SQLITE" } ]"""
            )
        }
    }

    @Test
    fun `a datasource name has to be a plain lowercase word`() {
        // It becomes a file name, a child-context bean name suffix and part of the plugin's
        // own vocabulary, so it is held to what the strictest of the three accepts
        listOf("Main", "main-1.0", "1main", "main/x", "main x", "", "a".repeat(65)).forEach {
            assertFailsWith<IllegalArgumentException>("'$it' should have been refused") {
                parseDatasources("""[ { "name": "$it", "dbType": "SQLITE" } ]""")
            }
        }
    }

    @Test
    fun `a property the datasource does not support is rejected`() {
        // The format is strict on purpose: a mistyped slot would otherwise be an authoring
        // mistake that silently never takes effect
        assertFailsWith<IllegalArgumentException> {
            parseDatasources("""[ { "name": "cache", "dbType": "SQLITE", "hosts": "db.host" } ]""")
        }
    }

    /**
     * Parse a `datasources` array against a group declaring every slot's field
     */
    private fun parseDatasources(datasources: String) =
        PluginConfigSchemaUtil.parse(
            """
            {
              "datasources": $datasources,
              "groups": [
                {
                  "key": "db",
                  "fields": [
                    { "key": "db.host", "type": "string" },
                    { "key": "db.port", "type": "number", "default": 3306, "minimum": 1, "maximum": 65535, "integer": true },
                    { "key": "db.name", "type": "string" },
                    { "key": "db.user", "type": "string" },
                    { "key": "db.password", "type": "secret" },
                    { "key": "db.params", "type": "string" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

    private val mysqlDatasource = """
        [
          {
            "name": "main",
            "dbType": "mysql",
            "required": true,
            "host": "db.host",
            "port": "db.port",
            "database": "db.name",
            "username": "db.user",
            "password": "db.password",
            "params": "db.params"
          }
        ]
    """.trimIndent()
}
