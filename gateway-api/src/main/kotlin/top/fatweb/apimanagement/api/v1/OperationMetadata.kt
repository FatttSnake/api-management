package top.fatweb.apimanagement.api.v1

/**
 * Metadata for one plugin operation.
 *
 * @property operationId stable identity within the plugin and API version
 * @property summary short display summary
 * @property description longer description
 * @property parameters declared parameters
 * @property requestBodySchema schema for the request body, or null
 * @property responses response specs by HTTP status code
 * @property tags OpenAPI tags
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class OperationMetadata @JvmOverloads constructor(
    val operationId: String,
    val summary: String = "",
    val description: String = "",
    val parameters: List<ParameterSpec> = emptyList(),
    val requestBodySchema: SchemaSpec? = null,
    val responses: Map<String, ResponseSpec> = emptyMap(),
    val tags: List<String> = emptyList()
) {
    init {
        require(operationId.isNotBlank()) { "operationId must not be blank" }
    }
}
