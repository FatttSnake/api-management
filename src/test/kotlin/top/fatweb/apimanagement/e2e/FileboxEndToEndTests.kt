package top.fatweb.apimanagement.e2e

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.exception.PluginDatasourceException
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigValueParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyAddParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateStatusParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyAddParam
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * End-to-end verification of the filebox example plugin against a running gateway.
 *
 * Disabled by default, and not a regression test: it installs a real plugin, creates a
 * real API key and writes to the configured dev database, so it is meant to be run
 * deliberately while verifying the plugin configuration, datasource and storage work.
 * Remove the annotation to run it (it needs the sibling `api-management-plugins`
 * repository checked out next to this one, with `filebox` built).
 *
 * The plugin's MySQL datasource is described from the gateway's own master connection,
 * pointing at a database of its own that this test creates - so the whole path is
 * exercised without a second server, and the gateway's own schema stays untouched.
 *
 * The server really listens, so every plugin call travels the whole path: the API key
 * filter, the access interceptor, the dispatcher and the plugin's own code. The admin
 * calls go through the same services the controllers delegate to, under an authenticated
 * principal, so the method security those controllers carry is applied as in production.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Disabled("installs a real plugin into the dev database; run deliberately")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FileboxEndToEndTests {
    private companion object {
        const val PLUGIN_ID = "filebox"
        const val JAR_NAME = "filebox-1.0.1.jar"

        /**
         * A plugin that is certainly not installed, for the "no such plugin" refusal
         */
        const val NO_SUCH_PLUGIN = "no-such-plugin"

        /**
         * Datasource holding the files, described by the administrator
         */
        const val MAIN = "main"

        /**
         * Datasource holding the snapshots, derived by the gateway
         */
        const val CACHE = "cache"

        /**
         * Database the plugin's MySQL datasource is pointed at, created and removed here
         */
        const val E2E_DATABASE = "filebox_e2e"

        /**
         * The sibling plugins repository, where the signed jar and its public key live
         */
        val PLUGIN_PROJECT: Path =
            Path.of(System.getProperty("user.dir")).parent.resolve("api-management-plugins").resolve("filebox")

        val CODES = listOf(
            "api:filebox:v1:upload",
            "api:filebox:v1:list",
            "api:filebox:v1:download",
            "api:filebox:v1:link",
            "api:filebox:v1:remove",
            "api:filebox:v1:snapshot",
            "api:filebox:v1:readSnapshot"
        )

        /**
         * What the administrator holds, i.e. what @PreAuthorize on the admin controllers checks
         */
        val ADMIN_AUTHORITIES = listOf(
            "system:plugin:key:add",
            "system:plugin:key:remove",
            "system:plugin:plugin:install",
            "system:plugin:plugin:query",
            "system:plugin:plugin:status",
            "system:plugin:plugin:uninstall",
            "system:plugin:plugin:reload",
            "system:plugin:config:query",
            "system:plugin:config:modify"
        )
    }

    @Value("\${local.server.port}")
    private var port: Int = 0

    @Value("\${spring.datasource.dynamic.datasource.master.url}")
    private lateinit var masterUrl: String

    @Value("\${spring.datasource.dynamic.datasource.master.username}")
    private lateinit var masterUsername: String

    @Value("\${spring.datasource.dynamic.datasource.master.password}")
    private lateinit var masterPassword: String

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Autowired
    private lateinit var apiPluginService: IApiPluginService

    @Autowired
    private lateinit var apiPluginTrustKeyService: IApiPluginTrustKeyService

    @Autowired
    private lateinit var apiPluginSettingService: IApiPluginSettingService

    @Autowired
    private lateinit var apiKeyService: IApiKeyService

    @Autowired
    private lateinit var serverProperties: ServerProperties

    private lateinit var trustKeyId: String
    private lateinit var accessKey: String
    private lateinit var secretKey: String
    private lateinit var fileId: String
    private var apiKeyId: Long = 0

    /**
     * Where the gateway keeps the databases of the plugin, as it composes them
     */
    private val datasourceDir: Path get() = Path.of(serverProperties.storage.pluginDatasourceDir, PLUGIN_ID)

    private val cacheFile: Path get() = datasourceDir.resolve("$CACHE.db")

    private val client: RestClient get() = RestClient.create("http://localhost:$port")

    /**
     * Start from a known state, so a run that failed halfway does not fail the next one
     */
    @BeforeAll
    fun resetPreviousRun() {
        if (apiPluginService.getByPluginId(PLUGIN_ID) != null) {
            asAdmin { apiPluginService.uninstallPlugin(PLUGIN_ID, purgeData = true) }
            println("[e2e] removed the plugin an earlier run left behind")
        }

        val leftovers = apiPluginTrustKeyService.get(ApiPluginTrustKeyGetParam(searchAlias = "filebox-e2e", enable = null))
        leftovers.records.forEach { asAdmin { apiPluginTrustKeyService.deleteByKeyId(it.keyId!!) } }

        // The plugin's own databases outlive an uninstall that was not asked to purge them,
        // and a run that failed halfway may have left some behind: the phases below count
        // what they upload, so they have to start from an empty one
        if (Files.exists(datasourceDir)) {
            Files.walk(datasourceDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    @Test
    @Order(1)
    fun `a plugin declaring two datasources installs and the sqlite one is ready`() {
        val publicKey = Files.readString(PLUGIN_PROJECT.resolve("keys/public.pem"))
        trustKeyId = asAdmin {
            apiPluginTrustKeyService.add(ApiPluginTrustKeyAddParam(publicKey = publicKey, alias = "filebox-e2e")).keyId!!
        }

        asAdmin {
            val vo = apiPluginService.installPlugin(
                Files.readAllBytes(PLUGIN_PROJECT.resolve("build/libs/$JAR_NAME")),
                JAR_NAME
            )

            assertEquals(PLUGIN_ID, vo.pluginId)
            assertEquals(null, vo.loadError)
        }

        val config = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }

        println("[e2e] declared groups: ${config.groups.map { it.key }}, fields: ${config.groups.sumOf { it.fields.size }}")
        assertEquals(listOf("db", "share", "advanced"), config.groups.map { it.key })
        assertEquals(11, config.groups.sumOf { it.fields.size })

        // Both datasources are declared, and the console can tell which of the declared
        // fields describe each one
        assertEquals(listOf(MAIN, CACHE), config.datasources.map { it.name })
        val main = config.datasources.first { it.name == MAIN }
        val cache = config.datasources.first { it.name == CACHE }
        assertEquals("MYSQL", main.dbType)
        assertEquals(true, main.required)
        assertEquals("SQLITE", cache.dbType)
        assertEquals(
            setOf("db.host", "db.port", "db.name", "db.user", "db.password", "db.params"),
            main.keys.toSet()
        )

        // Where to connect to has not been said yet, so the datasource is simply not there -
        // which is the normal state right after installing, not a failure
        assertEquals(false, main.configured)
        assertEquals(null, apiPluginService.getByPluginId(PLUGIN_ID)!!.loadError)

        // Deriving it is the whole configuration: nothing is left for an administrator to
        // fill in, so a SQLite datasource is already in place
        assertEquals(true, cache.configured)

        // The declared default is what an unset key reads as, and it is not a stored value
        val dbFields = config.groups.first { it.key == "db" }.fields
        val ttl = config.groups.first { it.key == "share" }.fields.first { it.key == "defaultLinkTtlHours" }
        assertEquals("3306", dbFields.first { it.key == "db.port" }.value)
        assertEquals(false, dbFields.first { it.key == "db.port" }.hasValue)
        assertEquals("1", ttl.value)
        assertEquals(false, ttl.hasValue)

        // A datasource the plugin does not declare cannot be tested, and a SQLite one has
        // nothing of the administrator's to test - it is a file the gateway supplies itself
        assertFailsWith<PluginDatasourceException> {
            asAdmin { apiPluginService.testPluginDatasource(PLUGIN_ID, CACHE, emptyMap()) }
        }
        if (apiPluginService.getByPluginId("echo") != null) {
            assertFailsWith<PluginDatasourceException> {
                asAdmin { apiPluginService.testPluginDatasource("echo", MAIN, emptyMap()) }
            }
            println("[e2e] a plugin that declares no datasource was refused a connection test")
        }

        // A plugin that is not installed is a different refusal from one that declares none
        assertFailsWith<NoRecordFoundException> {
            asAdmin { apiPluginService.testPluginDatasource(NO_SUCH_PLUGIN, MAIN, emptyMap()) }
        }
    }

    @Test
    @Order(2)
    fun `the sqlite database is derived per datasource and survives a reload`() {
        // One file per declared datasource, in a directory named after the plugin, so a
        // plugin with several SQLite ones cannot collide with itself
        assertTrue(
            Files.exists(cacheFile),
            "declaring a datasource should have provisioned $cacheFile without anyone configuring it"
        )

        asAdmin { apiPluginService.reloadPlugin(PLUGIN_ID) }

        assertTrue(Files.exists(cacheFile), "a reload must not lose the plugin's own data")
        println("[e2e] the derived datasource lives at $cacheFile and a reload left it alone")
    }

    @Test
    @Order(3)
    fun `describing the mysql datasource remounts the plugin without being asked to`() {
        createE2eDatabase()

        // A host that cannot be reached: the gateway refuses it before anything is stored,
        // because a value that commits and only then fails the mount would leave the plugin
        // down with nothing in the response to say why
        assertFailsWith<PluginDatasourceException> {
            asAdmin { saveMain(mainDatasourceValues(host = "127.0.0.1", port = "1")) }
        }
        val afterRefusal = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }
        assertEquals(
            false,
            afterRefusal.groups.first { it.key == "db" }.fields.first { it.key == "db.host" }.hasValue,
            "a refused configuration must not be stored"
        )

        val before = poolThreadNames().size
        println("[e2e] pool threads before describing the datasource: ${poolThreadNames()}")

        asAdmin { saveMain(mainDatasourceValues()) }

        // No reload is asked for anywhere above: the gateway builds the datasource, so it is
        // the gateway that remounts for it, and the administrator never has to know
        assertEquals(null, apiPluginService.getByPluginId(PLUGIN_ID)!!.loadError)
        val after = poolThreadNames().size
        println("[e2e] pool threads after describing the datasource:  ${poolThreadNames()}")
        assertTrue(after > before, "the new datasource should have brought a pool of its own")

        val config = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }
        assertEquals(true, config.datasources.first { it.name == MAIN }.configured)

        // A password is an ordinary secret field now, so it is encrypted at rest and comes
        // back as a mask rather than a value - and the mask goes back untouched to keep it
        val password = config.groups.first { it.key == "db" }.fields.first { it.key == "db.password" }
        assertEquals(true, password.secret)
        assertEquals(true, password.hasValue)
        assertTrue(
            password.value != masterPassword,
            "the API must not return the password, it returned '${password.value}'"
        )
        val stored = apiPluginSettingService.listByPlugin(PLUGIN_ID)["db.password"]!!
        assertTrue(stored != masterPassword, "the stored password must be ciphertext")
        println("[e2e] the stored password is ${stored.length} chars of ciphertext, and the API returns a mask")

        // Blanking the host is how a datasource is un-configured, and it is not an error:
        // everything else stays where it is, ready to be pointed somewhere again
        asAdmin {
            apiPluginService.updatePluginConfig(
                ApiPluginConfigUpdateParam(pluginId = PLUGIN_ID, values = listOf(ApiPluginConfigValueParam(key = "db.host", value = "")))
            )
        }
        val cleared = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }
        assertEquals(false, cleared.datasources.first { it.name == MAIN }.configured)
        assertEquals(null, apiPluginService.getByPluginId(PLUGIN_ID)!!.loadError, "a cleared host is not a broken one")
        println("[e2e] blanking the host un-configured the datasource without failing the mount")

        asAdmin { saveMain(mainDatasourceValues()) }
    }

    @Test
    @Order(4)
    fun `a connection can be tried before it is saved`() {
        // Against the gateway's own server, which this test knows is up
        asAdmin {
            apiPluginService.testPluginDatasource(PLUGIN_ID, MAIN, mainValuesByName())
        }

        // A connection test answers for the values it is given rather than for what is
        // stored, which is the whole point: a form can be tried out before it is saved
        assertFailsWith<PluginDatasourceException> {
            asAdmin {
                apiPluginService.testPluginDatasource(
                    PLUGIN_ID,
                    MAIN,
                    mapOf("db.host" to "127.0.0.1", "db.port" to "1")
                )
            }
        }

        // A value belonging to another datasource would describe something other than what
        // was named, so it is reported rather than quietly ignored
        assertFailsWith<PluginDatasourceException> {
            asAdmin {
                apiPluginService.testPluginDatasource(
                    PLUGIN_ID,
                    MAIN,
                    mapOf("db.host" to masterHost(), "allowSnapshot" to "true")
                )
            }
        }

        // The mask is what the console sends back for a secret it was never shown, so a test
        // has to work without the plaintext having been resubmitted
        asAdmin {
            apiPluginService.testPluginDatasource(
                PLUGIN_ID,
                MAIN,
                mapOf("db.host" to masterHost(), "db.password" to PluginConfigSchemaUtil.SECRET_MASK)
            )
        }
        println("[e2e] a connection was tried against submitted values, and against a masked password")
    }

    @Test
    @Order(5)
    fun `the plugin serves upload, list, download and a login-free link over http`() {
        val created = asAdmin {
            apiKeyService.add(
                managed = true,
                apiKeyAddParam = ApiKeyAddParam(
                    userId = null,
                    name = "filebox-e2e",
                    permissionCodes = CODES,
                    expireTime = null,
                    ipWhitelist = null,
                    rateLimit = null,
                    quota = null,
                    quotaPeriod = null,
                    remark = "temporary key created by the end-to-end verification"
                )
            )
        }
        apiKeyId = created.apiKey.id!!
        accessKey = created.apiKey.accessKey!!
        secretKey = created.secretKey!!
        println("[e2e] created API key ${created.apiKey.accessKey} with ${created.apiKey.permissions?.size} permission(s)")

        val body = "hello filebox ${System.currentTimeMillis()}".toByteArray()

        val uploaded = upload("/api/filebox/v1/upload", body)
        assertEquals(0, uploaded["code"], "the upload must be accepted: $uploaded")
        fileId = ((uploaded["data"] as Map<*, *>)["id"]) as String
        println("[e2e] upload -> $uploaded")

        val listed = call(HttpMethod.GET, "/api/filebox/v1/files")
        assertEquals(1, (listed["data"] as List<*>).size)

        val (downloadStatus, downloaded) = bytes("/api/filebox/v1/files/$fileId")
        assertEquals(200, downloadStatus)
        assertEquals(body.size, downloaded.size)
        assertTrue(body.contentEquals(downloaded), "the downloaded bytes must be the uploaded ones")

        val link = call(HttpMethod.GET, "/api/filebox/v1/files/$fileId/link")
        assertEquals(0, link["code"])
        val linkData = link["data"] as Map<*, *>
        val url = linkData["url"] as String
        println("[e2e] external link (${linkData["expiresInHours"]}h): $url")
        assertEquals(1L, (linkData["expiresInHours"] as Number).toLong(), "an unset ttl falls back to the declared default")

        // The link carries its own signature, so it needs no credential at all
        val (linkStatus, linked) = absoluteBytes(url)
        assertEquals(200, linkStatus)
        assertTrue(body.contentEquals(linked), "the signed link must serve the same bytes")
        println("[e2e] the link served the file without any credential")

        // A credential anyone can edit must not be trusted. The gateway answers a rejected
        // link with an error envelope rather than a bare status, so the check is on the
        // payload it actually serves.
        val (_, tampered) = absoluteBytes(url.replace(Regex("s=[^&]+"), "s=AAAA"))
        assertTrue(!body.contentEquals(tampered), "a tampered signature must not be served")
        assertTrue(tampered.decodeToString().contains("success\":false"), "the refusal should say so: ${tampered.decodeToString()}")

        // Deleting the file is the only way to revoke a link already handed out
        val removed = call(HttpMethod.DELETE, "/api/filebox/v1/files/$fileId")
        assertEquals(0, removed["code"])
        val (_, dead) = absoluteBytes(url)
        assertTrue(!body.contentEquals(dead), "a link to a deleted file must stop working")
        println("[e2e] a tampered signature was refused, and deleting the file killed the link")
    }

    @Test
    @Order(6)
    fun `a snapshot goes to the derived datasource and a config change needs no reload`() {
        val text = "snapshot body ${System.currentTimeMillis()}"

        val first = call(HttpMethod.POST, "/api/filebox/v1/snapshots?text=$text")
        val second = call(HttpMethod.POST, "/api/filebox/v1/snapshots?text=$text")

        assertEquals(0, first["code"])
        assertEquals(0, second["code"])
        val firstData = first["data"] as Map<*, *>
        val secondData = second["data"] as Map<*, *>
        assertEquals(firstData["contentKey"], secondData["contentKey"], "the same text hashes to one key")
        assertEquals(false, firstData["deduplicated"])
        assertEquals(true, secondData["deduplicated"], "the second write reuses the stored object")

        val readBack = call(HttpMethod.GET, "/api/filebox/v1/snapshots/${firstData["contentKey"]}")
        assertEquals(text, (readBack["data"] as Map<*, *>)["text"])

        // An ordinary configuration value is read per call, so it bites immediately - and
        // touching one that is not part of a datasource does not remount the plugin, which
        // is what keeps a plugin's in-memory state across an unrelated edit
        val before = poolThreadNames().size
        asAdmin {
            apiPluginService.updatePluginConfig(
                ApiPluginConfigUpdateParam(
                    pluginId = PLUGIN_ID,
                    values = listOf(ApiPluginConfigValueParam(key = "allowSnapshot", value = "false"))
                )
            )
        }
        assertEquals(before, poolThreadNames().size, "an ordinary configuration change must not remount the plugin")

        val disabled = call(HttpMethod.POST, "/api/filebox/v1/snapshots?text=another")
        assertEquals(2003, disabled["code"])
        println("[e2e] disabling snapshots took effect with no reload: $disabled")
    }

    @Test
    @Order(7)
    fun `a secret is stored encrypted, never returned, and read back as plaintext by the plugin`() {
        asAdmin {
            apiPluginService.updatePluginConfig(
                ApiPluginConfigUpdateParam(
                    pluginId = PLUGIN_ID,
                    values = listOf(ApiPluginConfigValueParam(key = "uploadToken", value = "e2e-token"))
                )
            )
        }

        val config = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }
        val secret = config.groups.flatMap { it.fields }.first { it.key == "uploadToken" }
        assertEquals(true, secret.secret)
        assertEquals(null, secret.value, "a secret must never be returned")
        assertEquals(true, secret.hasValue)

        val stored = apiPluginSettingService.listByPlugin(PLUGIN_ID)["uploadToken"]!!
        assertTrue(stored != "e2e-token", "the stored value must be ciphertext")
        println("[e2e] the stored secret is ${stored.length} chars of ciphertext, and the API returns hasValue only")

        assertEquals(0, upload("/api/filebox/v1/upload?token=e2e-token", "ok".toByteArray())["code"])
        assertEquals(2004, upload("/api/filebox/v1/upload", "nope".toByteArray())["code"])
        println("[e2e] the plugin read the decrypted secret and enforced it")

        // A blank secret clears it: the mask is what says "keep", and a submission that means
        // "there is no value" cannot also mean "leave the old one alone"
        asAdmin {
            apiPluginService.updatePluginConfig(
                ApiPluginConfigUpdateParam(
                    pluginId = PLUGIN_ID,
                    values = listOf(ApiPluginConfigValueParam(key = "uploadToken", value = ""))
                )
            )
        }
        assertEquals(null, apiPluginSettingService.listByPlugin(PLUGIN_ID)["uploadToken"])
        assertEquals(0, upload("/api/filebox/v1/upload", "ok".toByteArray())["code"])
        println("[e2e] a blank secret cleared it, and the upload no longer needed a token")

        asAdmin {
            apiPluginService.updatePluginConfig(
                ApiPluginConfigUpdateParam(
                    pluginId = PLUGIN_ID,
                    values = listOf(ApiPluginConfigValueParam(key = "uploadToken", value = "e2e-token"))
                )
            )
        }
    }

    @Test
    @Order(8)
    fun `reloads do not leak connection pools and keep the enable flag`() {
        val pluginRowId = apiPluginService.getByPluginId(PLUGIN_ID)!!.id!!

        asAdmin { apiPluginService.updatePluginStatus(ApiPluginUpdateStatusParam(id = pluginRowId, enable = false)) }

        println("[e2e] pool threads before: ${poolThreadNames()}")
        val before = poolThreadNames().size

        try {
            repeat(3) {
                asAdmin { apiPluginService.reloadPlugin(PLUGIN_ID) }
                Thread.sleep(500)
            }

            println("[e2e] pool threads after:  ${poolThreadNames()}")
            val after = poolThreadNames().size
            assertTrue(after <= before, "every reload must close the pool it replaces (before=$before after=$after)")

            assertEquals(0, apiPluginService.getByPluginId(PLUGIN_ID)!!.enable, "a reload must not re-enable a plugin")
            println("[e2e] a disabled plugin stayed disabled across 3 reloads")
        } finally {
            asAdmin { apiPluginService.updatePluginStatus(ApiPluginUpdateStatusParam(id = pluginRowId, enable = true)) }
        }
    }

    @Test
    @Order(9)
    fun `an uninstall keeps the configuration and data unless it is asked to purge`() {
        val tokenBefore = apiPluginSettingService.listByPlugin(PLUGIN_ID)["uploadToken"]
        val filesBefore = pluginFileCount()
        assertTrue(Files.exists(cacheFile), "the plugin's database should be there before the uninstall")
        assertTrue(filesBefore > 0, "the uploads should have left files behind")

        asAdmin { apiPluginService.reloadPlugin(PLUGIN_ID) }
        assertEquals(
            tokenBefore,
            apiPluginSettingService.listByPlugin(PLUGIN_ID)["uploadToken"],
            "a reload must not touch the configuration"
        )
        assertEquals(filesBefore, pluginFileCount())

        asAdmin { apiPluginService.uninstallPlugin(PLUGIN_ID, purgeData = false) }

        assertEquals(null, apiPluginService.getByPluginId(PLUGIN_ID), "the plugin row is gone after an uninstall")
        assertTrue(Files.exists(cacheFile), "purgeData=false must keep the plugin's own database")
        assertEquals(
            tokenBefore,
            apiPluginSettingService.listByPlugin(PLUGIN_ID)["uploadToken"],
            "purgeData=false must keep the settings"
        )
        assertEquals(filesBefore, pluginFileCount(), "purgeData=false must keep the stored files")
        println("[e2e] uninstall without purge kept the datasources, the settings and $filesBefore file(s)")

        asAdmin {
            apiPluginService.installPlugin(
                Files.readAllBytes(PLUGIN_PROJECT.resolve("build/libs/$JAR_NAME")),
                JAR_NAME
            )
        }

        // What the administrator configured is still there after a reinstall - including the
        // connection description, which is what makes the plugin work again immediately
        val reinstalled = asAdmin { apiPluginService.getPluginConfig(PLUGIN_ID) }
        val token = reinstalled.groups.flatMap { it.fields }.first { it.key == "uploadToken" }
        assertEquals(true, token.hasValue, "the configured secret must survive a reinstall")
        assertEquals(
            true,
            reinstalled.datasources.first { it.name == MAIN }.configured,
            "the datasource must survive a reinstall"
        )
        assertTrue(Files.exists(cacheFile), "the plugin's own database must survive a reinstall")
        println("[e2e] a reinstall picked the previous configuration back up")

        asAdmin { apiPluginService.uninstallPlugin(PLUGIN_ID, purgeData = true) }

        assertTrue(!Files.exists(datasourceDir), "purgeData=true must remove the whole datasource directory")
        assertEquals(emptyMap(), apiPluginSettingService.listByPlugin(PLUGIN_ID))
        assertEquals(0, pluginFileCount())
        println("[e2e] uninstall with purge removed the datasources, the settings and the stored files")
    }

    @Test
    @Order(10)
    fun `the temporary trust key, api key and database are removed`() {
        asAdmin { apiKeyService.removeById(apiKeyId) }
        asAdmin { apiPluginTrustKeyService.deleteByKeyId(trustKeyId) }

        // The gateway never drops tables in a database it does not own, so the one this test
        // created is cleaned up here rather than by the purge above
        DriverManager.getConnection("${masterServerUrl()}/$E2E_DATABASE", masterUsername, masterPassword).use { connection ->
            connection.createStatement().use { it.executeUpdate("drop table if exists t_p_filebox_file") }
        }
        println("[e2e] cleaned up the temporary API key, trust key and the plugin's tables")
    }

    /**
     * Save the six facts that describe the plugin's MySQL datasource
     */
    private fun saveMain(values: List<ApiPluginConfigValueParam>) {
        apiPluginService.updatePluginConfig(ApiPluginConfigUpdateParam(pluginId = PLUGIN_ID, values = values))
    }

    /**
     * The gateway's own MySQL server, as the plugin's file datasource
     */
    private fun mainDatasourceValues(host: String = masterHost(), port: String? = masterPort()) =
        listOfNotNull(
            ApiPluginConfigValueParam(key = "db.host", value = host),
            // Left out rather than submitted empty: a number field has no blank form, and the
            // driver's own default is what an unset port means
            port?.let { ApiPluginConfigValueParam(key = "db.port", value = it) },
            ApiPluginConfigValueParam(key = "db.name", value = E2E_DATABASE),
            ApiPluginConfigValueParam(key = "db.user", value = masterUsername),
            ApiPluginConfigValueParam(key = "db.password", value = masterPassword)
        )

    /**
     * The same facts keyed by config key, for a connection test
     */
    private fun mainValuesByName() = mainDatasourceValues().associate { it.key!! to it.value.orEmpty() }

    /**
     * Create the database the plugin's datasource points at
     */
    private fun createE2eDatabase() {
        DriverManager.getConnection("${masterServerUrl()}/", masterUsername, masterPassword).use { connection ->
            connection.createStatement().use { it.executeUpdate("create database if not exists $E2E_DATABASE") }
        }
    }

    private fun masterHost(): String = masterUrl.removePrefix("jdbc:mysql://").substringBefore('/').substringBefore(':')

    private fun masterPort(): String? =
        masterUrl.removePrefix("jdbc:mysql://").substringBefore('/').substringAfter(':', "").takeIf { it.isNotEmpty() }

    /**
     * The gateway's master URL without the database it names, for connecting to the server
     * itself rather than to one schema on it
     */
    private fun masterServerUrl(): String = "jdbc:mysql://" + masterUrl.removePrefix("jdbc:mysql://").substringBefore('/')

    /**
     * Run a block as the administrator, with the authorities the admin controllers require
     */
    private fun <T> asAdmin(block: () -> T): T {
        val user = User().apply {
            id = 0
            username = "admin"
            enable = 1
            operations = ADMIN_AUTHORITIES.map { code -> Operation().apply { this.code = code } }
        }
        val loginUser = LoginUser(user)
        val previous = SecurityContextHolder.getContext().authentication
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(loginUser, null, loginUser.authorities)

        return try {
            block()
        } finally {
            SecurityContextHolder.getContext().authentication = previous
        }
    }

    private fun authorization() =
        "Basic " + Base64.getEncoder().encodeToString("$accessKey:$secretKey".toByteArray())

    /**
     * Call a plugin endpoint with the API key, returning its status and decoded envelope
     */
    private fun call(method: HttpMethod, path: String): Map<*, *> {
        val (status, body) = exchange(method, path)

        return if (body.isBlank()) {
            mapOf("status" to status)
        } else {
            jsonMapper.readValue(body, Map::class.java) as Map<*, *>
        }
    }

    private fun exchange(method: HttpMethod, path: String): Pair<Int, String> =
        try {
            val response = client.method(method)
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .retrieve()
                .toEntity(String::class.java)

            response.statusCode.value() to (response.body ?: "")
        } catch (e: RestClientResponseException) {
            e.statusCode.value() to (e.responseBodyAsString ?: "")
        }

    /**
     * Upload a file the way a caller does: a multipart body with one `file` part, built
     * here so the request is exactly what the endpoint documents
     */
    private fun upload(path: String, body: ByteArray, fileName: String = "e2e.txt"): Map<*, *> {
        val boundary = "----fileboxE2eBoundary"
        val header = (
            "--$boundary\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n"
            ).toByteArray()
        val footer = "\r\n--$boundary--\r\n".toByteArray()

        return try {
            val response = client.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=$boundary"))
                .body(header + body + footer)
                .retrieve()
                .toEntity(String::class.java)

            jsonMapper.readValue(response.body!!, Map::class.java) as Map<*, *>
        } catch (e: RestClientResponseException) {
            jsonMapper.readValue(e.responseBodyAsString, Map::class.java) as Map<*, *>
        }
    }

    private fun bytes(path: String): Pair<Int, ByteArray> =
        try {
            val response = client.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .retrieve()
                .toEntity(ByteArray::class.java)

            response.statusCode.value() to (response.body ?: ByteArray(0))
        } catch (e: RestClientResponseException) {
            e.statusCode.value() to ByteArray(0)
        }

    /**
     * Fetch an absolute URL with no credential at all, the way a browser follows a link
     */
    private fun absoluteBytes(url: String): Pair<Int, ByteArray> =
        try {
            val response = client.get().uri(url).retrieve().toEntity(ByteArray::class.java)

            response.statusCode.value() to (response.body ?: ByteArray(0))
        } catch (e: RestClientResponseException) {
            e.statusCode.value() to ByteArray(0)
        }

    /**
     * Threads of every connection pool alive in the JVM, by name
     */
    private fun poolThreadNames(): List<String> =
        Thread.getAllStackTraces().keys.map { it.name }.filter { it.contains("housekeeper") }.sorted()

    private fun pluginFileCount(): Int {
        val root = Path.of(System.getProperty("user.dir"), "data/files/plugin-data/$PLUGIN_ID")

        return if (!Files.exists(root)) {
            0
        } else {
            Files.walk(root).use { paths -> paths.filter { Files.isRegularFile(it) }.count().toInt() }
        }
    }
}
