package top.fatweb.apimanagement.api.v1

/**
 * One JSON Schema node for a request or response body.
 *
 * @property type JSON Schema type: `object`, `array`, `string`, `integer`, `number` or `boolean`
 * @property description schema description
 * @property format JSON Schema format, for example `int64` or `date-time`
 * @property properties object properties, keyed by property name
 * @property required required property names
 * @property items array item schema
 * @property enumValues allowed values
 * @property example example value
 * @property nullable whether null is allowed
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class SchemaSpec @JvmOverloads constructor(
    val type: String = "object",
    val description: String = "",
    val format: String? = null,
    val properties: Map<String, SchemaSpec> = emptyMap(),
    val required: List<String> = emptyList(),
    val items: SchemaSpec? = null,
    val enumValues: List<Any> = emptyList(),
    val example: Any? = null,
    val nullable: Boolean = false,
) {
    companion object {
        /**
         * Builds a string schema.
         *
         * @param description schema description
         * @param format JSON Schema format
         * @param example example value
         * @param nullable whether null is allowed
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun string(
            description: String = "",
            format: String? = null,
            example: Any? = null,
            nullable: Boolean = false,
        ): SchemaSpec = SchemaSpec("string", description, format, example = example, nullable = nullable)

        /**
         * Builds an integer schema.
         *
         * @param description schema description
         * @param format JSON Schema format
         * @param example example value
         * @param nullable whether null is allowed
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun integer(
            description: String = "",
            format: String? = null,
            example: Any? = null,
            nullable: Boolean = false,
        ): SchemaSpec = SchemaSpec("integer", description, format, example = example, nullable = nullable)

        /**
         * Builds a number schema.
         *
         * @param description schema description
         * @param format JSON Schema format
         * @param example example value
         * @param nullable whether null is allowed
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun number(
            description: String = "",
            format: String? = null,
            example: Any? = null,
            nullable: Boolean = false,
        ): SchemaSpec = SchemaSpec("number", description, format, example = example, nullable = nullable)

        /**
         * Builds a boolean schema.
         *
         * @param description schema description
         * @param example example value
         * @param nullable whether null is allowed
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun boolean(description: String = "", example: Any? = null, nullable: Boolean = false): SchemaSpec =
            SchemaSpec("boolean", description, example = example, nullable = nullable)

        /**
         * Builds an array schema.
         *
         * @param items item schema
         * @param description schema description
         * @param nullable whether null is allowed
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun array(items: SchemaSpec, description: String = "", nullable: Boolean = false): SchemaSpec =
            SchemaSpec("array", description, items = items, nullable = nullable)

        /**
         * Builds an object schema.
         *
         * @param properties object properties
         * @param required required property names
         * @param description schema description
         * @return schema
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun objectOf(
            properties: Map<String, SchemaSpec>,
            required: List<String> = emptyList(),
            description: String = "",
        ): SchemaSpec = SchemaSpec("object", description, properties = properties, required = required)
    }
}
