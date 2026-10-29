package top.fatweb.apimanagement.component.plugin

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
    SECRET,

    ;

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
        fun parse(raw: String): PluginConfigFieldType? = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
