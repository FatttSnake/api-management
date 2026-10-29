package top.fatweb.apimanagement.api.v1

/**
 * One declared operation parameter.
 *
 * @property name parameter name
 * @property location where the parameter is read from
 * @property type JSON Schema type, for example `string`, `integer` or `boolean`
 * @property required whether the parameter is required
 * @property description parameter description
 * @property format JSON Schema format, for example `int64` or `date-time`
 * @property enumValues allowed values
 * @property example example value
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ParameterSpec @JvmOverloads constructor(
    val name: String,
    val location: ParameterLocation,
    val type: String = "string",
    val required: Boolean = false,
    val description: String = "",
    val format: String? = null,
    val enumValues: List<Any> = emptyList(),
    val example: Any? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
    }

    companion object {
        /**
         * Builds a required path parameter.
         *
         * @param name parameter name
         * @param type JSON Schema type
         * @param description parameter description
         * @param example example value
         * @return parameter
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun path(
            name: String,
            type: String = "string",
            description: String = "",
            example: Any? = null,
        ): ParameterSpec = ParameterSpec(
            name = name,
            location = ParameterLocation.PATH,
            type = type,
            required = true,
            description = description,
            example = example,
        )

        /**
         * Builds a query parameter.
         *
         * @param name parameter name
         * @param type JSON Schema type
         * @param required whether the parameter is required
         * @param description parameter description
         * @param example example value
         * @return parameter
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        @JvmOverloads
        fun query(
            name: String,
            type: String = "string",
            required: Boolean = false,
            description: String = "",
            example: Any? = null,
        ): ParameterSpec = ParameterSpec(
            name = name,
            location = ParameterLocation.QUERY,
            type = type,
            required = required,
            description = description,
            example = example,
        )
    }
}
