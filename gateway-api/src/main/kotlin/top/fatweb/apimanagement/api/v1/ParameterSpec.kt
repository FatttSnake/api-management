package top.fatweb.apimanagement.api.v1

/**
 * One declared operation parameter.
 *
 * @property name parameter name
 * @property location where the parameter is read from
 * @property type JSON Schema type, for example `string`, `integer` or `boolean`
 * @property required whether the parameter is required
 * @property description parameter description
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ParameterSpec @JvmOverloads constructor(
    val name: String,
    val location: ParameterLocation,
    val type: String = "string",
    val required: Boolean = false,
    val description: String = ""
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
         * @return parameter
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun path(name: String, type: String = "string"): ParameterSpec =
            ParameterSpec(name, ParameterLocation.PATH, type, true)

        /**
         * Builds a query parameter.
         *
         * @param name parameter name
         * @param type JSON Schema type
         * @param required whether the parameter is required
         * @return parameter
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun query(name: String, type: String = "string", required: Boolean = false): ParameterSpec =
            ParameterSpec(name, ParameterLocation.QUERY, type, required)
    }
}
