package top.fatweb.apimanagement.component.plugin

/**
 * Plugin datasource declaration
 *
 * A plugin asking for its own isolated database, and nothing more than the asking: the
 * connection itself is declared as ordinary config fields, which [slots] points at.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginDatasourceSchema(
    /**
     * Name the plugin refers to this datasource by, unique inside the schema
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val name: String,
    /**
     * Whether the plugin cannot run without this datasource
     *
     * Reported as an incomplete configuration rather than enforced: the plugin still installs
     * and mounts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val required: Boolean,
    /**
     * Database type the plugin is written against
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val dbType: PluginDatasourceType,
    /**
     * Config key holding each connection fact, for the slots this dialect uses
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val slots: Map<PluginDatasourceSlot, String>,
) {
    companion object {
        /**
         * Datasource name pattern
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val NAME_REGEX = Regex("^[a-z][a-z0-9-]*$")

        /**
         * Longest datasource name
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val MAX_NAME_LENGTH = 64

        /**
         * Check whether a datasource name is one the gateway can compose a resource from
         *
         * @param name Datasource name
         * @return true=the name is usable
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun isValidName(name: String): Boolean = name.length <= MAX_NAME_LENGTH && NAME_REGEX.matches(name)
    }
}

/**
 * Plugin datasource slot
 *
 * The connection facts the gateway composes a datasource from; the JDBC URL itself is never
 * one of them.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class PluginDatasourceSlot {
    /**
     * Server the database listens on
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    HOST,

    /**
     * Port it listens on; the driver's own default applies when unset
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    PORT,

    /**
     * Database on that server
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    DATABASE,

    /**
     * Credential username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    USERNAME,

    /**
     * Credential password; held as a secret field, so it is encrypted at rest and
     * never returned by the API
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    PASSWORD,

    /**
     * Driver properties, as `key=value` pairs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    PARAMS,

    ;

    companion object {
        /**
         * Parse a declared slot
         *
         * @param raw Slot as written in the schema
         * @return Slot, or null when unknown
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(raw: String): PluginDatasourceSlot? = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

/**
 * Plugin datasource type
 *
 * Decides how the gateway supplies the datasource.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class PluginDatasourceType {
    /**
     * External MySQL server, described by the administrator's settings
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    MYSQL,

    /**
     * Local SQLite file, derived and supplied by the gateway
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    SQLITE,

    ;

    companion object {
        /**
         * Parse a declared database type
         *
         * @param raw Type as written in the schema
         * @return Database type, or null when unknown
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(raw: String): PluginDatasourceType? = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
