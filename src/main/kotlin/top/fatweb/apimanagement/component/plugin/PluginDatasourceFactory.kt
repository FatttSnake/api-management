package top.fatweb.apimanagement.component.plugin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.exception.PluginDatasourceException
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.PluginDatasourceUtil
import top.fatweb.apimanagement.util.ResolvedDatasource
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.Comparator
import java.util.Properties
import javax.sql.DataSource

/**
 * Plugin datasource factory
 *
 * Builds the isolated datasources a plugin declares, from the config values an
 * administrator owns. Holds every collaborator the build needs, so the plugin service
 * that calls it stays free of them - the same shape as [PluginStorageFactory], which
 * supplies the other resource a plugin is given.
 *
 * The gateway's own datasource is never involved: a plugin gets one of these or none.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see PluginSettingReader
 * @see PluginConfigSchemaCache
 */
@Component
class PluginDatasourceFactory(
    private val serverProperties: ServerProperties,
    private val pluginSettingReader: PluginSettingReader,
    private val pluginConfigSchemaCache: PluginConfigSchemaCache
) {
    companion object {
        /**
         * Seconds a connection test waits before it declares the server unreachable
         */
        private const val CONNECT_TIMEOUT_SECONDS = 3

        /**
         * Same limit in milliseconds, which is the form the driver takes it in
         */
        private const val CONNECT_TIMEOUT_MILLIS = CONNECT_TIMEOUT_SECONDS * 1000
    }

    /**
     * Directory the gateway keeps the databases of SQLite datasources in
     */
    private val datasourceDir: String get() = serverProperties.storage.pluginDatasourceDir

    /**
     * Build every datasource a plugin declares
     *
     * A declared datasource that is not configured is left out rather than reported: a
     * plugin is installed before its server's details are known, and the plugin is told
     * about the absence through `PluginContext.datasources` so it can say so clearly
     * instead of failing to mount.
     *
     * Building a pool does not connect, so an unreachable server costs nothing here and
     * the plugin still mounts.
     *
     * @param pluginId Plugin ID
     * @param schema Schema the plugin declared, or null when it declared none
     * @return Datasources by name, empty when the plugin declares none
     * @throws PluginDatasourceException when a configured value cannot describe a connection
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     * @see DataSource
     */
    fun create(pluginId: String, schema: PluginConfigSchema?): Map<String, DataSource> =
        schema?.datasources.orEmpty().mapNotNull { declaration ->
            val resolved = PluginDatasourceUtil.resolve(declaration) { key ->
                pluginSettingReader.resolve(pluginId, key)
            } ?: return@mapNotNull null

            declaration.name to sourceOf(pluginId, resolved)
        }.toMap()

    /**
     * Verify that a datasource can be connected to
     *
     * Asked for rather than done while saving: a configuration is worth storing before its
     * server is reachable, and the administrator is the one who decides when the question
     * is worth asking. Submitted values are used where they were submitted, so a
     * configuration can be tried out before it is saved.
     *
     * A plain driver connection rather than a pool: this answers "is it reachable" in one
     * round trip and holds nothing afterwards.
     *
     * No timeout is set on the JVM: [DriverManager] holds its login timeout in a static
     * that every other connection in the process shares, so the limit is handed to the
     * driver per connection instead.
     *
     * @param pluginId Plugin ID
     * @param name Datasource name
     * @param values Submitted values, keyed by config key; a key that is absent or null is
     *        read from what is stored
     * @throws PluginDatasourceException when the datasource is undeclared, not configurable,
     *         not configured, described by values that do not belong to it, or unreachable
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun test(pluginId: String, name: String, values: Map<String, String?>) {
        val schema = pluginConfigSchemaCache.get(pluginId)
            ?: throw PluginDatasourceException("Plugin '$pluginId' declares no configuration")

        val declaration = schema.datasourceOf(name)
            ?: throw PluginDatasourceException("Plugin '$pluginId' does not declare a datasource named '$name'")

        // Nothing is connected to, so there is nothing to test: the file is created when the
        // gateway supplies it, and a test would only create one early
        if (declaration.dbType == PluginDatasourceType.SQLITE) {
            throw PluginDatasourceException(
                "Datasource '$name' is a SQLITE datasource, which the gateway supplies itself and which" +
                    " cannot be tested"
            )
        }

        // A value that belongs to another datasource means the caller is describing something
        // other than what it named, which is worth reporting rather than ignoring
        val foreign = values.keys - declaration.slots.values.toSet()
        if (foreign.isNotEmpty()) {
            throw PluginDatasourceException(
                "Datasource '$name' is not described by ${foreign.joinToString()}"
            )
        }

        // A value the caller left out, or sent with no value, is the stored one: a form that
        // was not filled in is testing the configuration the plugin is running with
        val resolved = PluginDatasourceUtil.resolve(declaration) { key ->
            values[key] ?: pluginSettingReader.resolve(pluginId, key)
        } as? ResolvedDatasource.Mysql
            ?: throw PluginDatasourceException("Datasource '$name' is not configured")

        connect(resolved)
    }

    /**
     * Delete everything the gateway keeps for a plugin's datasources
     *
     * The whole directory rather than the files of the datasources currently declared:
     * uninstalling with a purge wants a plugin's databases gone, including those of a
     * declaration that has since changed.
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun removeData(pluginId: String) {
        require(StorageKeyUtil.PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }

        val directory = Path.of(datasourceDir, pluginId)
        if (!Files.exists(directory)) {
            return
        }

        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    /**
     * Build the pool of a resolved datasource
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResolvedDatasource
     * @see DataSource
     */
    private fun sourceOf(pluginId: String, resolved: ResolvedDatasource): DataSource = when (resolved) {
        // SQLite creates the file itself but not the directory it goes in
        is ResolvedDatasource.Sqlite -> {
            val file = PluginDatasourceUtil.sqliteFile(datasourceDir, pluginId, resolved.name)
            file.parent?.let { Files.createDirectories(it) }

            pool(
                pluginId = pluginId,
                name = resolved.name,
                driver = PluginDatasourceUtil.SQLITE_DRIVER,
                url = PluginDatasourceUtil.sqliteUrl(datasourceDir, pluginId, resolved.name),
                username = null,
                password = null,
                params = Properties()
            )
        }

        is ResolvedDatasource.Mysql -> pool(
            pluginId = pluginId,
            name = resolved.name,
            driver = PluginDatasourceUtil.MYSQL_DRIVER,
            url = PluginDatasourceUtil.mysqlUrl(resolved.host, resolved.port, resolved.database),
            username = resolved.username,
            password = resolved.password,
            params = resolved.params
        )
    }

    /**
     * Build one connection pool
     *
     * Built explicitly rather than through `DataSourceBuilder`, because two of the settings
     * below are the point: how many connections a plugin may hold open, and that building a
     * pool must not require the server to be up.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Properties
     * @see DataSource
     */
    private fun pool(
        pluginId: String,
        name: String,
        driver: String,
        url: String,
        username: String?,
        password: String?,
        params: Properties
    ): DataSource {
        val config = HikariConfig()
        config.driverClassName = driver
        config.jdbcUrl = url
        username?.let { config.username = it }
        // An empty password still has to be handed over: a server that needs none is a
        // different server from one that needs the right one, and only the driver can tell
        config.password = password.orEmpty()
        // Driver properties rather than URL parameters: what a server needs beyond its
        // defaults is a property of the connection, and a property cannot rewrite the URL
        // it belongs to
        config.dataSourceProperties = params
        config.poolName = "plugin-$pluginId-$name"
        config.maximumPoolSize = serverProperties.storage.pluginDatasourcePoolSize
        config.minimumIdle = 0
        // Mounting happens whether or not the database is reachable right now: a server that
        // is down, or a firewall that is not open yet, must not make a plugin disappear
        config.initializationFailTimeout = -1

        return HikariDataSource(config)
    }

    /**
     * Verify that a MySQL datasource can be connected to
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResolvedDatasource
     */
    private fun connect(mysql: ResolvedDatasource.Mysql) {
        val params = Properties()
        params.putAll(mysql.params)
        params["user"] = mysql.username.orEmpty()
        params["password"] = mysql.password.orEmpty()
        params["connectTimeout"] = CONNECT_TIMEOUT_MILLIS.toString()

        try {
            DriverManager.getConnection(
                PluginDatasourceUtil.mysqlUrl(mysql.host, mysql.port, mysql.database),
                params
            ).use { connection ->
                if (!connection.isValid(CONNECT_TIMEOUT_SECONDS)) {
                    throw PluginDatasourceException(
                        "The datasource did not answer within $CONNECT_TIMEOUT_SECONDS s"
                    )
                }
            }
        } catch (e: PluginDatasourceException) {
            throw e
        } catch (e: Exception) {
            throw PluginDatasourceException("Cannot connect to the datasource: ${e.message}")
        }
    }
}
