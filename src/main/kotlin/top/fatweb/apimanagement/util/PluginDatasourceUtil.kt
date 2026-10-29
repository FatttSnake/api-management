package top.fatweb.apimanagement.util

import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSlot
import top.fatweb.apimanagement.component.plugin.PluginDatasourceType
import top.fatweb.apimanagement.exception.PluginDatasourceException
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.file.Path
import java.util.Properties

/**
 * Plugin datasource util
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginDatasourceUtil {
    /**
     * Driver of a MySQL datasource
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver"

    /**
     * Driver of a SQLite datasource
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val SQLITE_DRIVER = "org.sqlite.JDBC"

    /**
     * Extension of the file backing a SQLite datasource
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val SQLITE_SUFFIX = ".db"

    /**
     * Highest TCP port
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private const val MAX_PORT = 65535

    /**
     * Accepted host in the shapes a server is named in: a name or an IPv4 address,
     * and nothing that could rewrite the authority it is composed into
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private val HOST_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9.-]*$")

    /**
     * Accepted IPv6 literal, optionally carrying a zone
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private val IPV6_REGEX = Regex("^[0-9A-Fa-f:.]+(%[A-Za-z0-9]+)?$")

    /**
     * Accepted database name: what MySQL itself allows, minus anything that could end
     * the path segment it is composed into
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private val DATABASE_REGEX = Regex("^[A-Za-z0-9_$-]+$")

    /**
     * Accepted driver parameters: one or more `key=value` pairs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val PARAMS_REGEX = Regex("^[A-Za-z0-9_.-]+=[^&\\s]*(&[A-Za-z0-9_.-]+=[^&\\s]*)*$")

    /**
     * Longest driver property string
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private const val MAX_PARAMS_LENGTH = 500

    /**
     * Driver properties the gateway refuses, in lowercase
     *
     * @see parseParams
     */
    private val PARAM_DENY_LIST =
        setOf(
            "autodeserialize",
            "allowloadlocalinfile",
            "allowloadlocalinfileinpath",
            "allowurlinlocalinfile",
            "socketfactory",
            "socketfactoryarg",
            "propertiestransform",
            "useconfigs",
        )

    /**
     * Compose the JDBC URL of a MySQL datasource
     *
     * @param host Database host
     * @param port Database port, or null to let the driver's own default apply
     * @param database Database name
     * @return JDBC URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun mysqlUrl(host: String, port: Int?, database: String): String =
        "jdbc:mysql://${authorityOf(host, port)}/$database"

    /**
     * Compose the JDBC URL of a SQLite datasource
     *
     * Forward slashes on every platform, because the value goes to a JDBC driver rather
     * than the file system API.
     *
     * @param directory Directory the gateway keeps plugin databases in
     * @param pluginId Plugin ID
     * @param name Datasource name
     * @return JDBC URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sqliteUrl(directory: String, pluginId: String, name: String): String =
        "jdbc:sqlite:" + sqliteFile(directory, pluginId, name).toString().replace('\\', '/')

    /**
     * Resolve the file backing the SQLite datasource of a plugin
     *
     * @param directory Directory the gateway keeps plugin databases in
     * @param pluginId Plugin ID
     * @param name Datasource name
     * @return Database file
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sqliteFile(directory: String, pluginId: String, name: String): Path {
        require(PluginIdUtil.REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }
        require(PluginDatasourceSchema.isValidName(name)) { "Invalid datasource name: '$name'" }

        return Path.of(directory, pluginId, "$name$SQLITE_SUFFIX")
    }

    /**
     * Resolve a declared datasource from the values behind it
     *
     * @param datasource Datasource declaration
     * @param valueOf Value of a config key, resolved through its field default
     * @return Connection description, or null when a required slot is blank and there is
     *         nothing to connect with
     * @throws PluginDatasourceException when a submitted value cannot describe a connection
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(datasource: PluginDatasourceSchema, valueOf: (String) -> String?): ResolvedDatasource? {
        // Deriving it is the whole configuration: there is nothing here for anyone to fill in
        if (datasource.dbType == PluginDatasourceType.SQLITE) {
            return ResolvedDatasource.Sqlite(datasource.name)
        }

        if (!isConfigured(datasource, valueOf)) {
            return null
        }

        val slotValue = { slot: PluginDatasourceSlot -> datasource.slots[slot]?.let(valueOf) }

        return ResolvedDatasource.Mysql(
            name = datasource.name,
            host = requireHost(slotValue(PluginDatasourceSlot.HOST)),
            port = portOf(slotValue(PluginDatasourceSlot.PORT)),
            database = requireDatabase(slotValue(PluginDatasourceSlot.DATABASE)),
            username = slotValue(PluginDatasourceSlot.USERNAME)?.trim()?.takeIf { it.isNotEmpty() },
            password = slotValue(PluginDatasourceSlot.PASSWORD)?.takeIf { it.isNotEmpty() },
            params = parseParams(requireParams(slotValue(PluginDatasourceSlot.PARAMS))),
        )
    }

    /**
     * Check whether a declared datasource is configured
     *
     * A SQLite one is configured from the moment it is declared; a MySQL one once both the host
     * and the database it connects to are there. A required slot left blank is what
     * un-configures it - and an un-configured datasource is simply absent, never an error.
     *
     * @param datasource Datasource declaration
     * @param valueOf Value of a config key, resolved through its field default
     * @return true=the datasource is configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isConfigured(datasource: PluginDatasourceSchema, valueOf: (String) -> String?): Boolean =
        when (datasource.dbType) {
            // Supplied the moment it is declared, so there is nothing to be missing
            PluginDatasourceType.SQLITE -> true

            // Not the host alone: a form the administrator has only begun filling in is one the
            // gateway still has nothing to connect with, and reading it as configured is what
            // turns a fresh install, or a half-filled save, into an error with no way past
            PluginDatasourceType.MYSQL ->
                !datasource.slots[PluginDatasourceSlot.HOST]?.let(valueOf).isNullOrBlank() &&
                    !datasource.slots[PluginDatasourceSlot.DATABASE]?.let(valueOf).isNullOrBlank()
        }

    /**
     * Resolve a declared datasource from the values behind it, by name
     *
     * @param schema Schema the datasource is declared by
     * @param name Datasource name
     * @param valueOf Value of a config key, resolved through its field default
     * @return Connection description, or null when not configured
     * @throws PluginDatasourceException when the plugin declares no such datasource, or a
     *         value that is there cannot describe a connection
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(schema: PluginConfigSchema, name: String, valueOf: (String) -> String?): ResolvedDatasource? {
        val datasource =
            schema.datasourceOf(name)
                ?: throw PluginDatasourceException("Plugin does not declare a datasource named '$name'")

        return resolve(datasource, valueOf)
    }

    /**
     * Read driver properties from a submitted parameter string
     *
     * @param raw Validated parameter string, or null when none were submitted
     * @return Driver properties, empty when none were submitted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parseParams(raw: String?): Properties {
        val params = Properties()
        if (raw.isNullOrEmpty()) {
            return params
        }

        raw.split('&').forEach { pair ->
            val key = pair.substringBefore('=')

            params.setProperty(key, pair.substringAfter('=', ""))
        }

        return params
    }

    /**
     * Compose the authority of a URL
     *
     * A literal is bracketed here although the stored value is unbracketed.
     *
     * @param host Database host
     * @param port Database port, or null when unset
     * @return Authority
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun authorityOf(host: String, port: Int?): String {
        val authority = if (host.contains(':')) "[$host]" else host

        return if (port == null) authority else "$authority:$port"
    }

    /**
     * Resolve the host to connect to
     *
     * Reached only once [isConfigured] says there is one, which is why a blank is left out
     * here rather than refused.
     *
     * @param raw Submitted host
     * @return Normalised host
     * @throws PluginDatasourceException when the host cannot be one
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireHost(raw: String?): String {
        val submitted = raw?.trim().orEmpty()

        val host = submitted.removeSurrounding("[", "]")
        val accepted =
            if (host.contains(':')) {
                IPV6_REGEX.matches(host) &&
                    runCatching { InetAddress.getByName(host) is Inet6Address }.getOrDefault(false)
            } else {
                HOST_REGEX.matches(host)
            }
        if (!accepted) {
            throw PluginDatasourceException(
                "Database host must be a name, an address or an IPv6 literal, got '$submitted'",
            )
        }

        return host
    }

    /**
     * Resolve the port to connect to
     *
     * @param raw Submitted port
     * @return Validated port, or null when none was submitted
     * @throws PluginDatasourceException when the port is not a whole number in range
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun portOf(raw: String?): Int? {
        val submitted = raw?.trim().orEmpty()
        if (submitted.isEmpty()) {
            return null
        }

        // Read as a decimal rather than an int: a number field accepts a trailing zero, so
        // "3306.0" is a value an administrator can legitimately have stored
        val number =
            submitted.toBigDecimalOrNull()?.takeIf { it.stripTrailingZeros().scale() <= 0 }
                ?: throw PluginDatasourceException("Database port must be a whole number, got '$submitted'")
        val port = number.toLong()
        if (port !in 1..MAX_PORT.toLong()) {
            throw PluginDatasourceException("Database port must be between 1 and $MAX_PORT, got '$submitted'")
        }

        return port.toInt()
    }

    /**
     * Resolve the database name to connect to
     *
     * Reached only once [isConfigured] says there is one, which is why a blank is left out
     * here rather than refused.
     *
     * @param raw Submitted database name
     * @return Validated database name
     * @throws PluginDatasourceException when the name cannot be one
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireDatabase(raw: String?): String {
        val database = raw?.trim().orEmpty()
        if (!DATABASE_REGEX.matches(database)) {
            throw PluginDatasourceException("Database name must match ${DATABASE_REGEX.pattern}, got '$database'")
        }

        return database
    }

    /**
     * Resolve the driver parameters to hand the driver
     *
     * @param raw Submitted parameters
     * @return Validated parameters, or null when none were submitted
     * @throws PluginDatasourceException when they are too long, not `key=value` pairs, or
     *         name a property the gateway refuses
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireParams(raw: String?): String? {
        val params = raw?.trim().orEmpty()
        if (params.isEmpty()) {
            return null
        }
        if (params.length > MAX_PARAMS_LENGTH) {
            throw PluginDatasourceException(
                "Database parameters are longer than $MAX_PARAMS_LENGTH characters (${params.length})",
            )
        }
        if (!PARAMS_REGEX.matches(params)) {
            throw PluginDatasourceException(
                "Database parameters must be one or more 'key=value' pairs separated by '&', got '$params'",
            )
        }

        val refused =
            params
                .split('&')
                .map { it.substringBefore('=').lowercase() }
                .filter { it in PARAM_DENY_LIST }
        if (refused.isNotEmpty()) {
            throw PluginDatasourceException(
                "Database parameters name properties the gateway refuses: ${refused.joinToString()}",
            )
        }

        return params
    }
}

/**
 * Plugin datasource as the gateway is about to connect with it
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
sealed interface ResolvedDatasource {
    /**
     * Name the plugin refers to this datasource by
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val name: String

    /**
     * Dialect the plugin declared
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val dbType: PluginDatasourceType

    /**
     * A SQLite database the gateway owns
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Sqlite(override val name: String) : ResolvedDatasource {
        override val dbType: PluginDatasourceType get() = PluginDatasourceType.SQLITE
    }

    /**
     * An external MySQL server
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Mysql(
        override val name: String,
        /**
         * Host to connect to, unbracketed whatever the address family
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val host: String,
        /**
         * Port to connect to, or null to let the driver's own default apply
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val port: Int?,
        /**
         * Database on that server
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val database: String,
        /**
         * Credential username, or null when none was configured
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val username: String?,
        /**
         * Credential password in plaintext, or null when none was configured
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val password: String?,
        /**
         * Driver properties, never part of the URL
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val params: Properties,
    ) : ResolvedDatasource {
        override val dbType: PluginDatasourceType get() = PluginDatasourceType.MYSQL
    }
}
