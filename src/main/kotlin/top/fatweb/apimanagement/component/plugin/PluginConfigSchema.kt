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
    val groups: List<PluginConfigFieldGroup>,
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
    fun secretKeys(): Set<String> = fields.mapNotNullTo(mutableSetOf()) {
        if (it.type ==
            PluginConfigFieldType.SECRET
        ) {
            it.key
        } else {
            null
        }
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
    val fields: List<PluginConfigField>,
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
    val options: List<PluginConfigFieldOption>?,
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
    val label: String?,
)
