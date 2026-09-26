package top.fatweb.apimanagement.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSlot
import top.fatweb.apimanagement.component.plugin.PluginDatasourceType
import top.fatweb.apimanagement.exception.PluginDatasourceException
import java.nio.file.Path

/**
 * Plugin datasource util tests
 *
 * What the gateway makes of a datasource declaration and the config values behind it, and
 * what it refuses those values for. The rules are the gateway's own rather than the
 * plugin's: a field's declared constraints are its author's convenience, and a value that
 * reaches the driver can rewrite a connection in ways a form never shows.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginDatasourceUtil
 */
class PluginDatasourceUtilTests {
    @Test
    fun `a mysql url is composed from its parts`() {
        assertEquals(
            "jdbc:mysql://10.0.0.240:3306/filebox",
            PluginDatasourceUtil.mysqlUrl("10.0.0.240", 3306, "filebox")
        )
        assertEquals(
            "jdbc:mysql://db.internal:3307/other",
            PluginDatasourceUtil.mysqlUrl("db.internal", 3307, "other")
        )
    }

    @Test
    fun `a url leaves out the port it was not given`() {
        // Nothing is defaulted here: the driver's own default is the only thing that decides
        // a port nobody named, so the gateway does not become a second source of truth
        assertEquals("jdbc:mysql://10.0.0.240/filebox", PluginDatasourceUtil.mysqlUrl("10.0.0.240", null, "filebox"))
    }

    @Test
    fun `an ipv6 host is bracketed in the url`() {
        // The brackets are URL syntax rather than part of the address, so they go on here
        // rather than in what the administrator submitted or what is stored
        assertEquals("jdbc:mysql://[::1]:3306/filebox", PluginDatasourceUtil.mysqlUrl("::1", 3306, "filebox"))
        assertEquals(
            "jdbc:mysql://[2001:db8::1]/filebox",
            PluginDatasourceUtil.mysqlUrl("2001:db8::1", null, "filebox")
        )
    }

    @Test
    fun `a datasource is not configured until it says where to connect to`() {
        // An administrator installs a plugin before they have the server's details, so a
        // declaration with no host is a datasource that is simply not there yet - never an
        // error they cannot get past
        assertNull(mysql(host = null))
        assertNull(mysql(host = ""))
        assertNull(mysql(host = "   "))

        assertFalse(PluginDatasourceUtil.isConfigured(mysqlSchema()) { null })
        assertTrue(PluginDatasourceUtil.isConfigured(mysqlSchema()) { "10.0.0.240" })
    }

    @Test
    fun `a datasource with no host is unconfigured whatever else was filled in`() {
        // The other facts are read only once there is somewhere to connect to, so a partly
        // filled form is not a state of its own
        assertNull(mysql(host = null, database = "filebox", username = "filebox"))
    }

    @Test
    fun `a configured datasource carries every fact through`() {
        val resolved = mysql(username = "filebox", password = "secret", params = "useSSL=false")

        assertEquals("main", resolved?.name)
        assertEquals(PluginDatasourceType.MYSQL, resolved?.dbType)
        assertEquals("10.0.0.240", resolved?.host)
        assertEquals(3306, resolved?.port)
        assertEquals("filebox", resolved?.database)
        assertEquals("filebox", resolved?.username)
        assertEquals("secret", resolved?.password)
        assertEquals("false", resolved?.params?.getProperty("useSSL"))
    }

    @Test
    fun `a host is accepted in the shapes a server is named in`() {
        assertEquals("db.internal", mysql(host = "db.internal")?.host)
        assertEquals("localhost", mysql(host = "  localhost  ")?.host)
        // Brackets are how a literal is written inside a URL, so a submission that has them
        // is understood rather than refused - and is stored without them
        assertEquals("::1", mysql(host = "[::1]")?.host)
        assertEquals("::1", mysql(host = "::1")?.host)
        assertEquals("2001:db8::1", mysql(host = "2001:db8::1")?.host)
    }

    @Test
    fun `a host cannot rewrite the url it goes into`() {
        // Every one of these would move the connection somewhere other than the host the
        // administrator named, or end the authority section of the URL early
        listOf("host/../other", "host:1234", "host?useSSL=false", "ho st", "host#fragment", "user@host")
            .forEach {
                assertFailsWith<PluginDatasourceException>("'$it' should have been refused") {
                    mysql(host = it)
                }
            }
    }

    @Test
    fun `a port has to be one a server could listen on`() {
        assertFailsWith<PluginDatasourceException> { mysql(port = "0") }
        assertFailsWith<PluginDatasourceException> { mysql(port = "-1") }
        assertFailsWith<PluginDatasourceException> { mysql(port = "65536") }
        assertFailsWith<PluginDatasourceException> { mysql(port = "33a6") }
        assertFailsWith<PluginDatasourceException> { mysql(port = "10.5") }

        // A number field accepts a trailing zero, so a stored 3306.0 is a value an
        // administrator can legitimately have, and refusing it here would break their form
        assertEquals(3306, mysql(port = "3306.0")?.port)
        assertEquals(65535, mysql(port = "65535")?.port)
        // Left out, the driver decides - which is a different thing from storing a guess
        assertNull(mysql(port = null)?.port)
    }

    @Test
    fun `a database cannot rewrite the url it goes into`() {
        listOf("", "file box", "filebox/other", "filebox?x=1", "filebox;drop").forEach {
            assertFailsWith<PluginDatasourceException>("'$it' should have been refused") {
                mysql(database = it)
            }
        }

        assertFailsWith<PluginDatasourceException> { mysql(database = null) }
        assertEquals("filebox", mysql(database = "filebox")?.database)
        assertEquals("plugin_db\$1", mysql(database = "plugin_db\$1")?.database)
    }

    @Test
    fun `driver parameters become connection properties`() {
        val params = PluginDatasourceUtil.parseParams("useSSL=false&serverTimezone=UTC")

        assertEquals("false", params.getProperty("useSSL"))
        assertEquals("UTC", params.getProperty("serverTimezone"))
        assertTrue(PluginDatasourceUtil.parseParams(null).isEmpty())
    }

    @Test
    fun `a property value may contain the separator of a pair`() {
        // MySQL's own sessionVariables relies on it
        val params = PluginDatasourceUtil.parseParams("sessionVariables=sql_mode=ANSI_QUOTES")

        assertEquals("sql_mode=ANSI_QUOTES", params.getProperty("sessionVariables"))
    }

    @Test
    fun `driver parameters have to be key value pairs`() {
        listOf("useSSL", "=false", "use SSL=false", "useSSL=false other", "useSSL=false&", "&useSSL=false").forEach {
            assertFailsWith<PluginDatasourceException>("'$it' should have been refused") {
                mysql(params = it)
            }
        }

        assertNull(mysql(params = null)?.params?.takeIf { !it.isEmpty() })
        assertNull(mysql(params = "  ")?.params?.takeIf { !it.isEmpty() })
    }

    @Test
    fun `driver parameters cannot exceed the value that holds them`() {
        assertFailsWith<PluginDatasourceException> { mysql(params = "a=" + "b".repeat(600)) }
    }

    @Test
    fun `a property that reaches beyond a connection is refused`() {
        // These name a class to load, a file to read or another property set to pull in, and
        // a property reaches the driver as directly as it would have the URL. The list is a
        // mitigation rather than a sandbox, but it has to actually hold
        listOf(
            "autoDeserialize=true",
            "AUTODESERIALIZE=true",
            "allowLoadLocalInfile=true",
            "allowLoadLocalInfileInPath=/etc",
            "allowUrlInLocalInfile=true",
            "socketFactory=com.example.Factory",
            "socketFactoryArg=x",
            "propertiesTransform=com.example.Transform",
            "useConfigs=evil"
        ).forEach {
            assertFailsWith<PluginDatasourceException>("'$it' should have been refused") {
                mysql(params = it)
            }
        }

        // A refused property does not take the harmless ones next to it down with it
        assertFailsWith<PluginDatasourceException> { mysql(params = "useSSL=false&allowLoadLocalInfile=true") }
    }

    @Test
    fun `a sqlite file is derived from the plugin id and the datasource name`() {
        val file = PluginDatasourceUtil.sqliteFile("data/db/plugin", "filebox", "cache")

        assertEquals(Path.of("data/db/plugin", "filebox", "cache.db"), file)
        assertEquals("data/db/plugin/filebox/cache.db", file.toString().replace('\\', '/'))
        assertEquals(
            "jdbc:sqlite:data/db/plugin/filebox/cache.db",
            PluginDatasourceUtil.sqliteUrl("data/db/plugin", "filebox", "cache")
        )
    }

    @Test
    fun `a plugin id cannot escape the database directory`() {
        // The directory name is the plugin ID, so this is the one check between a plugin ID
        // and an arbitrary path - it is composed here rather than trusted from the descriptor
        listOf("../filebox", "a/b", "FileBox", "file box", "", "..").forEach {
            assertFailsWith<IllegalArgumentException>("'$it' should have been refused") {
                PluginDatasourceUtil.sqliteFile("data/db/plugin", it, "cache")
            }
        }
    }

    @Test
    fun `a datasource name cannot escape the database directory`() {
        // Likewise for the file name, which comes from a declaration rather than from the
        // administrator - so it is the plugin's own mistake if it is not a plain word
        listOf("../cache", "a/b", "Cache", "cache.db", "a b", "", "cache/../x").forEach {
            assertFailsWith<IllegalArgumentException>("'$it' should have been refused") {
                PluginDatasourceUtil.sqliteFile("data/db/plugin", "filebox", it)
            }
        }
    }

    @Test
    fun `a sqlite datasource needs no configuration at all`() {
        val resolved = PluginDatasourceUtil.resolve(
            PluginDatasourceSchema("cache", required = false, dbType = PluginDatasourceType.SQLITE, slots = emptyMap())
        ) { null }

        assertEquals("cache", resolved?.name)
        assertEquals(PluginDatasourceType.SQLITE, resolved?.dbType)
        assertTrue(
            PluginDatasourceUtil.isConfigured(
                PluginDatasourceSchema("cache", required = false, dbType = PluginDatasourceType.SQLITE, slots = emptyMap())
            ) { null }
        )
    }

    /**
     * Resolve a MySQL datasource declaring every slot, from the values given
     */
    private fun mysql(
        host: String? = "10.0.0.240",
        port: String? = "3306",
        database: String? = "filebox",
        username: String? = null,
        password: String? = null,
        params: String? = null
    ): ResolvedDatasource.Mysql? {
        val values = mapOf(
            "db.host" to host,
            "db.port" to port,
            "db.name" to database,
            "db.user" to username,
            "db.password" to password,
            "db.params" to params
        )

        return PluginDatasourceUtil.resolve(mysqlSchema()) { values[it] } as? ResolvedDatasource.Mysql
    }

    /**
     * Declaration naming every slot of a MySQL datasource
     */
    private fun mysqlSchema() = PluginDatasourceSchema(
        name = "main",
        required = true,
        dbType = PluginDatasourceType.MYSQL,
        slots = mapOf(
            PluginDatasourceSlot.HOST to "db.host",
            PluginDatasourceSlot.PORT to "db.port",
            PluginDatasourceSlot.DATABASE to "db.name",
            PluginDatasourceSlot.USERNAME to "db.user",
            PluginDatasourceSlot.PASSWORD to "db.password",
            PluginDatasourceSlot.PARAMS to "db.params"
        )
    )
}
