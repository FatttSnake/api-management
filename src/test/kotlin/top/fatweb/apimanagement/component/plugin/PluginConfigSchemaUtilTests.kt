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
            submit(
                "share",
                "ttlHours" to "3",
                "enabled" to "false",
                "mode" to "slow",
                "notice" to "hello",
                "token" to "s3cr3t"
            )
        )
    }

    @Test
    fun `a value for an undeclared key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "nope" to "1"))
        }
    }

    @Test
    fun `a key declared by another group is rejected`() {
        // Writing a key through a group that does not own it would be a field changing from
        // under the administrator who can only see the group they submitted
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("elsewhere", "ttlHours" to "3"))
        }
    }

    @Test
    fun `an undeclared group is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("nowhere", "token" to "x"))
        }
    }

    @Test
    fun `a key that was left out is not checked at all`() {
        // Nothing was decided about it, so what is stored stands - even if it would not pass
        // the field's own constraints today
        PluginConfigSchemaUtil.validate(
            schema,
            submit("share", "notice" to null),
            stored = mapOf("token" to "s3cr3t")
        )
    }

    @Test
    fun `a value of the wrong type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "enabled" to "yes", "token" to "x"))
        }
    }

    @Test
    fun `a number outside its range or with a fraction is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "ttlHours" to "169", "token" to "x"))
        }
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "ttlHours" to "1.5", "token" to "x"))
        }
    }

    @Test
    fun `an enum value outside its options is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "mode" to "medium", "token" to "x"))
        }
    }

    @Test
    fun `a value longer than its maximum is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "notice" to "too long", "token" to "x"))
        }
    }

    @Test
    fun `a value larger than the column is refused`() {
        // A backstop rather than a declared rule: the setting column is a `text`, so a field
        // that declares no maximum of its own is still bounded by what can be stored
        val unbounded = parseField("""{ "key": "a", "type": "text" }""")!!

        assertEquals(PluginConfigFieldType.TEXT, unbounded.fieldOf("a")!!.type)
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(unbounded, submit("g", "a" to "a".repeat(70_000)))
        }
        PluginConfigSchemaUtil.validate(unbounded, submit("g", "a" to "a".repeat(65_000)))
    }

    @Test
    fun `a blank clears a key that has no blank form to store`() {
        // A number, a boolean and an enumeration have nothing to store a blank as, so blanking
        // one is how an administrator gets back to the declared default: the row goes rather
        // than holding an empty string the type could never read back
        listOf("ttlHours", "enabled", "mode", "token").forEach { key ->
            val field = schema.fieldOf(key)!!

            assertEquals(PluginConfigValueAction.KEEP, PluginConfigSchemaUtil.actionOf(field, null), key)
            assertEquals(PluginConfigValueAction.CLEAR, PluginConfigSchemaUtil.actionOf(field, ""), key)
            assertEquals(PluginConfigValueAction.SET, PluginConfigSchemaUtil.actionOf(field, "x"), key)
            assertEquals(
                field.default,
                PluginConfigSchemaUtil.effectiveValue(field, "", storedValue = "stored"),
                "a cleared '$key' should fall back to its declared default"
            )
        }
    }

    @Test
    fun `a blank is a value to a text field`() {
        // Which is what makes a text field the one kind that cannot be emptied: an empty string
        // is something it can hold, and the way back to its default is to write the default
        val notice = schema.fieldOf("notice")!!

        assertEquals(PluginConfigValueAction.KEEP, PluginConfigSchemaUtil.actionOf(notice, null))
        assertEquals(PluginConfigValueAction.SET, PluginConfigSchemaUtil.actionOf(notice, ""))
        assertEquals("", PluginConfigSchemaUtil.effectiveValue(notice, "", storedValue = "hello"))
        PluginConfigSchemaUtil.validate(schema, submit("share", "notice" to "", "token" to "s3cr3t"))
    }

    @Test
    fun `a text field holds a blank to its own constraints`() {
        // A field declaring a shortest length is saying a blank is not a value it accepts
        val constrained = parseField("""{ "key": "a", "type": "string", "minLength": 3 }""")!!

        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.validate(constrained, submit("g", "a" to "")) }
    }

    @Test
    fun `a blank clears a secret without checking it against the field constraints`() {
        // Clearing leaves no value behind, so a length or a pattern the field declares has
        // nothing left to apply to
        val constrained = parseField("""{ "key": "token", "type": "secret", "minLength": 20 }""")!!

        PluginConfigSchemaUtil.validate(constrained, submit("g", "token" to ""))
    }

    @Test
    fun `a required field is satisfied by a stored value or a declared default`() {
        // The secret is the only required field here, and it has no default to fall back to,
        // so a save that decides about it has to leave one behind
        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(schema, submit("share", "notice" to "hi"))
        }

        PluginConfigSchemaUtil.validate(
            schema,
            submit("share", "notice" to "hi"),
            stored = mapOf("token" to "s3cr3t")
        )
    }

    @Test
    fun `a required field of a group nobody submitted is not checked`() {
        // A console saves one group at a time, so a field left unfinished in another group is
        // not this save's business - otherwise the form could never be saved at all
        val twoGroups = PluginConfigSchemaUtil.parse(
            """
            {
              "groups": [
                { "key": "db", "fields": [ { "key": "db.host", "type": "string", "required": true } ] },
                { "key": "share", "fields": [ { "key": "notice", "type": "string" } ] }
              ]
            }
            """.trimIndent()
        )!!

        PluginConfigSchemaUtil.validate(twoGroups, submit("share", "notice" to "hi"))
        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.validate(twoGroups, submit("db", "db.host" to "")) }
    }

    @Test
    fun `a blank is not a value a required field can be satisfied by`() {
        // A blank clears a key, so a required one is left unsatisfied rather than set to
        // nothing - whatever is still stored under it
        val required = PluginConfigSchemaUtil.parse(
            """
            { "groups": [ { "key": "g", "fields": [ { "key": "a", "type": "string", "required": true } ] } ] }
            """.trimIndent()
        )!!

        assertFailsWith<IllegalArgumentException> { PluginConfigSchemaUtil.validate(required, submit("g", "a" to "")) }
        PluginConfigSchemaUtil.validate(required, submit("g", "a" to "x"))

        assertFailsWith<IllegalArgumentException> {
            PluginConfigSchemaUtil.validate(
                schema,
                submit("share", "token" to ""),
                stored = mapOf("token" to "s3cr3t")
            )
        }
    }

    @Test
    fun `the values after a save are read from the submission, the stored values and the defaults`() {
        val effective = PluginConfigSchemaUtil.effectiveValues(
            schema,
            submit("share", "ttlHours" to "", "enabled" to "false", "notice" to ""),
            stored = mapOf("ttlHours" to "5", "mode" to "slow", "token" to "s3cr3t")
        )

        assertEquals("2", effective["ttlHours"], "a cleared key falls back to its declared default")
        assertEquals("false", effective["enabled"], "a submitted value is what is read")
        assertEquals("slow", effective["mode"], "a key that was left out keeps what is stored")
        assertEquals("", effective["notice"], "a blank is what a text field reads back")
        assertEquals("s3cr3t", effective["token"], "a secret that was left out is the stored one")

        // Nothing is stored under it and it declares no default, so leaving it alone leaves it
        // with nothing at all
        assertNull(PluginConfigSchemaUtil.effectiveValues(schema, emptyMap(), emptyMap())["notice"])
    }

    private fun parseField(fields: String) =
        PluginConfigSchemaUtil.parse("""{ "groups": [ { "key": "g", "fields": [ $fields ] } ] }""")

    /**
     * Submit values to one group, in the shape the API takes them
     *
     * A null value is a key that was left out of the request.
     */
    private fun submit(groupKey: String, vararg values: Pair<String, String?>) =
        mapOf(groupKey to values.toMap())

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
