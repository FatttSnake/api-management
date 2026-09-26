package top.fatweb.apimanagement.util

import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSlot
import top.fatweb.apimanagement.component.plugin.PluginDatasourceType
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.exception.PluginDatasourceException
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.file.Path
import java.util.Properties

/**
 * Plugin datasource util
 *
 * Owns how the gateway turns a plugin's datasource declaration and the config values
 * behind it into a connection. Deliberately pure and free of Spring, so the rules that
 * keep a submitted value from rewriting the connection it is composed into can be
 * tested without a container.
 *
 * The connection is composed rather than accepted: what the administrator submits is
 * held to the declared field's own constraints first, and to these rules afterwards -
 * a declared pattern is the plugin author's convenience, not a guarantee.
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
     */
    private const val MAX_PORT = 65535

    /**
     * Accepted host in the shapes a server is named in: a name or an IPv4 address,
     * and nothing that could rewrite the authority it is composed into
     */
    private val HOST_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9.-]*$")

    /**
     * Accepted IPv6 literal, optionally carrying a zone
     *
     * The charset alone would admit nonsense, so a match is confirmed by resolving it -
     * which never reaches the network for a literal containing a colon.
     */
    private val IPV6_REGEX = Regex("^[0-9A-Fa-f:.]+(%[A-Za-z0-9]+)?$")

    /**
     * Accepted database name: what MySQL itself allows, minus anything that could end
     * the path segment it is composed into
     */
    private val DATABASE_REGEX = Regex("^[A-Za-z0-9_$-]+$")

    /**
     * Accepted driver parameters: one or more `key=value` pairs
     *
     * The value may itself contain `=`, which MySQL properties such as
     * `sessionVariables` rely on, but nothing that could end the pair.
     */
    private val PARAMS_REGEX = Regex("^[A-Za-z0-9_.-]+=[^&\\s]*(&[A-Za-z0-9_.-]+=[^&\\s]*)*$")

    /**
     * Longest driver property string
     *
     * The value is stored in `t_b_api_plugin_setting.setting_value`, which is a `text`
     * column; the limit is here so a form cannot hand the driver something absurd.
     */
    private const val MAX_PARAMS_LENGTH = 500

    /**
     * Driver properties the gateway refuses, in lowercase
     *
     * Held as connection properties rather than appended to the URL, which removes the
     * URL-rewriting surface but not this one: a property reaches the driver just as
     * directly. These are the ways a property string names a class to load, a file to
     * read or another property set to pull in - so a connection the administrator
     * describes cannot become one that fetches code. The list is a mitigation and not
     * a sandbox: the boundary that actually holds is the plugin signature and the trust
     * store behind it.
     *
     * @see parseParams
     */
    private val PARAM_DENY_LIST = setOf(
        "autodeserialize",
        "allowloadlocalinfile",
        "allowloadlocalinfileinpath",
        "allowurlinlocalinfile",
        "socketfactory",
        "socketfactoryarg",
        "propertiestransform",
        "useconfigs"
    )

    /**
     * Compose the JDBC URL of a MySQL datasource
     *
     * The gateway appends nothing of its own: the driver's defaults are used as they
     * are, so it never silently downgrades a transport to make a connection work. What
     * a server needs beyond its defaults is described by the properties of
     * [ResolvedDatasource.Mysql], which never reach the URL.
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
     * Forward slashes on every platform: the URL is handed to a JDBC driver, not to the
     * file system API of the machine it happens to run on.
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
     * Each plugin owns a directory named after it and each datasource a file inside that
     * directory, so a plugin with several of them cannot collide with itself. Both
     * segments are checked again here, as the last line of defence before a path is
     * composed from them.
     *
     * @param directory Directory the gateway keeps plugin databases in
     * @param pluginId Plugin ID
     * @param name Datasource name
     * @return Database file
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Path
     */
    fun sqliteFile(directory: String, pluginId: String, name: String): Path {
        require(StorageKeyUtil.PLUGIN_ID_REGEX.matches(pluginId)) { "Invalid plugin ID: '$pluginId'" }
        require(PluginDatasourceSchema.isValidName(name)) { "Invalid datasource name: '$name'" }

        return Path.of(directory, pluginId, "$name$SQLITE_SUFFIX")
    }

    /**
     * Resolve a declared datasource from the values behind it
     *
     * The one entry point for turning a declaration into a connection description, and
     * therefore the one place the gateway's own rules about those values live: the save
     * path calls it to reject a configuration it could never connect with, and the mount
     * calls it to build the pools.
     *
     * Returns null rather than throwing when the datasource is simply not configured -
     * an administrator installing a plugin before they have the server's details is the
     * normal order, and a datasource the gateway cannot describe is one the plugin does
     * not get. A value that *is* there and is malformed throws instead: silently
     * dropping it would hide a broken connection behind a working-looking plugin.
     *
     * @param datasource Datasource declaration
     * @param valueOf Value of a config key, resolved through its field default
     * @return Connection description, or null when the datasource is not configured
     * @throws PluginDatasourceException when a submitted value cannot describe a connection
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginDatasourceSchema
     * @see ResolvedDatasource
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
            params = parseParams(requireParams(slotValue(PluginDatasourceSlot.PARAMS)))
        )
    }

    /**
     * Check whether a declared datasource is configured
     *
     * One rule decides it - whether the host resolves to a value - which is what keeps a
     * half-filled form from being a state of its own: everything else is read only once
     * there is somewhere to connect to. Blanking the host is therefore how a datasource is
     * un-configured, and a plugin reading `PluginContext.datasources` simply loses the
     * entry.
     *
     * This never throws, so a read can report the state of a configuration the gateway
     * would refuse to connect with rather than failing to show it at all.
     *
     * @param datasource Datasource declaration
     * @param valueOf Value of a config key, resolved through its field default
     * @return true=the datasource is configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginDatasourceSchema
     */
    fun isConfigured(datasource: PluginDatasourceSchema, valueOf: (String) -> String?): Boolean =
        when (datasource.dbType) {
            // Supplied the moment it is declared, so there is nothing to be missing
            PluginDatasourceType.SQLITE -> true

            PluginDatasourceType.MYSQL ->
                !datasource.slots[PluginDatasourceSlot.HOST]?.let(valueOf).isNullOrBlank()
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
     * @see PluginConfigSchema
     * @see ResolvedDatasource
     */
    fun resolve(
        schema: PluginConfigSchema,
        name: String,
        valueOf: (String) -> String?
    ): ResolvedDatasource? {
        val datasource = schema.datasourceOf(name)
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
     * @see Properties
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
     * A literal has to be bracketed inside a URL while the stored value is unbracketed,
     * since the brackets are URL syntax rather than part of the address. The port is
     * omitted rather than defaulted, so the driver's own default is the only thing that
     * decides it.
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
     * @param raw Submitted host
     * @return Normalised host
     * @throws PluginDatasourceException when the host is missing or malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireHost(raw: String?): String {
        val submitted = raw?.trim().orEmpty()
        if (submitted.isEmpty()) {
            throw PluginDatasourceException("Database host can not be blank")
        }

        val host = submitted.removeSurrounding("[", "]")
        val accepted = if (host.contains(':')) {
            IPV6_REGEX.matches(host) && runCatching { InetAddress.getByName(host) is Inet6Address }.getOrDefault(false)
        } else {
            HOST_REGEX.matches(host)
        }
        if (!accepted) {
            throw PluginDatasourceException(
                "Database host must be a name, an address or an IPv6 literal, got '$submitted'"
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
        val number = submitted.toBigDecimalOrNull()?.takeIf { it.stripTrailingZeros().scale() <= 0 }
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
     * @param raw Submitted database name
     * @return Validated database name
     * @throws PluginDatasourceException when the name is missing or malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireDatabase(raw: String?): String {
        val database = raw?.trim().orEmpty()
        if (database.isEmpty()) {
            throw PluginDatasourceException("Database name can not be blank")
        }
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
                "Database parameters are longer than $MAX_PARAMS_LENGTH characters (${params.length})"
            )
        }
        if (!PARAMS_REGEX.matches(params)) {
            throw PluginDatasourceException(
                "Database parameters must be one or more 'key=value' pairs separated by '&', got '$params'"
            )
        }

        val refused = params.split('&')
            .map { it.substringBefore('=').lowercase() }
            .filter { it in PARAM_DENY_LIST }
        if (refused.isNotEmpty()) {
            throw PluginDatasourceException(
                "Database parameters name properties the gateway refuses: ${refused.joinToString()}"
            )
        }

        return params
    }
}

/**
 * Plugin datasource as the gateway is about to connect with it
 *
 * The result of applying the gateway's own rules to a declaration and the values behind
 * it, and the last thing that happens before a pool is built or a connection tested.
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
     * @see PluginDatasourceType
     */
    val dbType: PluginDatasourceType

    /**
     * A SQLite database the gateway owns
     *
     * Carries nothing but its name: where the file goes is decided by the gateway's own
     * configuration rather than by anything an administrator or a plugin submits.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResolvedDatasource
     */
    data class Sqlite(override val name: String) : ResolvedDatasource {
        override val dbType: PluginDatasourceType get() = PluginDatasourceType.SQLITE
    }

    /**
     * An external MySQL server
     *
     * Every fact here was a config value the administrator owns, read back through the
     * rules of the field that declared it.
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
        val params: Properties
    ) : ResolvedDatasource {
        override val dbType: PluginDatasourceType get() = PluginDatasourceType.MYSQL
    }
}
