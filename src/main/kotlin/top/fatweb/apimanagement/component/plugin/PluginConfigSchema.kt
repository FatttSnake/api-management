package top.fatweb.apimanagement.component.plugin

import java.math.BigDecimal

/**
 * Plugin config schema
 *
 * The parsed declaration of the settings a plugin owns, shipped inside the plugin's own jar as
 * `META-INF/plugin-config.json` and snapshotted onto the plugin row at mount time. A key
 * declared here is administrator-owned and never holds a current value.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginConfigSchema(
    /**
     * Datasource declarations in declaration order; empty when the plugin uses no
     * database of its own
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val datasources: List<PluginDatasourceSchema>,

    /**
     * Config field groups in declaration order
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val groups: List<PluginConfigFieldGroup>
) {
    /**
     * Every declared field, flattened across [groups], in declaration order
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val fields: List<PluginConfigField> = groups.flatMap { it.fields }

    private val fieldsByKey: Map<String, PluginConfigField> = fields.associateBy { it.key }

    private val groupsByKey: Map<String, PluginConfigFieldGroup> = groups.associateBy { it.key }

    private val datasourcesByName: Map<String, PluginDatasourceSchema> =
        datasources.associateBy { it.name }

    /**
     * Every config key that holds a connection slot of some datasource
     *
     * A change to one of these is what a remount is needed for; a plugin that is down from a
     * failed mount is remounted on any save instead.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val datasourceKeys: Set<String> = datasources.flatMapTo(mutableSetOf()) { it.slots.values }

    /**
     * Check whether a key is declared by this schema
     *
     * @param key Config key
     * @return true=declared (administrator-owned); false=not declared (plugin-owned)
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isDeclared(key: String): Boolean = fieldsByKey.containsKey(key)

    /**
     * Get a declared field
     *
     * @param key Config key
     * @return Field declaration, or null when not declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fieldOf(key: String): PluginConfigField? = fieldsByKey[key]

    /**
     * Get a declared group
     *
     * @param key Group key
     * @return Group declaration, or null when not declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun groupOf(key: String): PluginConfigFieldGroup? = groupsByKey[key]

    /**
     * Get a declared datasource by name
     *
     * @param name Datasource name
     * @return Datasource declaration, or null when not declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun datasourceOf(name: String): PluginDatasourceSchema? = datasourcesByName[name]

    /**
     * Get the default of a declared field
     *
     * @param key Config key
     * @return Declared default, or null when the field is undeclared or has none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun defaultOf(key: String): String? = fieldsByKey[key]?.default

    /**
     * Check whether a key holds a secret
     *
     * @param key Config key
     * @return true=the value is encrypted at rest and never returned by the API
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isSecret(key: String): Boolean = fieldsByKey[key]?.type == PluginConfigFieldType.SECRET

    /**
     * Get every key that holds a secret
     *
     * @return Secret keys
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun secretKeys(): Set<String> =
        fields.mapNotNullTo(mutableSetOf()) { if (it.type == PluginConfigFieldType.SECRET) it.key else null }
}

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
    val slots: Map<PluginDatasourceSlot, String>
) {
    companion object {
        /**
         * Datasource name pattern
         */
        val NAME_REGEX = Regex("^[a-z][a-z0-9-]*$")

        /**
         * Longest datasource name
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
        fun isValidName(name: String): Boolean =
            name.length <= MAX_NAME_LENGTH && NAME_REGEX.matches(name)
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
    PARAMS;

    companion object {
        /**
         * Parse a declared slot
         *
         * @param raw Slot as written in the schema
         * @return Slot, or null when unknown
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(raw: String): PluginDatasourceSlot? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
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
    SQLITE;

    companion object {
        /**
         * Parse a declared database type
         *
         * @param raw Type as written in the schema
         * @return Database type, or null when unknown
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(raw: String): PluginDatasourceType? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

/**
 * Plugin config field group
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginConfigFieldGroup(
    /**
     * Group key, unique inside the schema
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val key: String,

    /**
     * Display title
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val title: String?,

    /**
     * Display description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val description: String?,

    /**
     * Fields of this group, in declaration order
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val fields: List<PluginConfigField>
)

/**
 * Plugin config field
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginConfigField(
    /**
     * Config key; unique inside the schema and used as the setting key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val key: String,

    /**
     * Field type, driving validation and rendering
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val type: PluginConfigFieldType,

    /**
     * Display title
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val title: String?,

    /**
     * Display description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val description: String?,

    /**
     * Value used when the administrator has not set one; never stored as a row
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val default: String?,

    /**
     * Whether the field must resolve to a value
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val required: Boolean,

    /**
     * Input placeholder
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val placeholder: String?,

    /**
     * Lowest accepted value of a number field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val minimum: BigDecimal?,

    /**
     * Highest accepted value of a number field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val maximum: BigDecimal?,

    /**
     * Whether a number field rejects a fractional part
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val integer: Boolean,

    /**
     * Lowest accepted length of a string or text field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val minLength: Int?,

    /**
     * Highest accepted length of a string or text field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val maxLength: Int?,

    /**
     * Regular expression a string or text field must match
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pattern: String?,

    /**
     * Allowed values of an enum field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val options: List<PluginConfigFieldOption>?
)

/**
 * Plugin config field option
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginConfigFieldOption(
    /**
     * Value stored when the option is chosen
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val value: String,

    /**
     * Display label
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val label: String?
)

/**
 * Plugin config field type
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class PluginConfigFieldType {
    /**
     * Single-line text
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    STRING,

    /**
     * Multi-line text
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    TEXT,

    /**
     * Decimal number
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    NUMBER,

    /**
     * Boolean, stored as "true" / "false"
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    BOOLEAN,

    /**
     * One of a fixed set of values
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    ENUM,

    /**
     * Encrypted at rest, never returned by the API, read as plaintext by the plugin
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    SECRET;

    /**
     * Whether the type holds free text
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val isText: Boolean get() = this == STRING || this == TEXT

    companion object {
        /**
         * Parse a declared type
         *
         * @param raw Type as written in the schema
         * @return Field type, or null when unknown
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(raw: String): PluginConfigFieldType? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
