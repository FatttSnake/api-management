package top.fatweb.apimanagement.api.v1

/**
 * One declared response of an operation.
 *
 * @property description response description
 * @property schema schema of the response body, or null
 * @property example example response body
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ResponseSpec @JvmOverloads constructor(
    val description: String = "OK",
    val schema: SchemaSpec? = null,
    val example: Any? = null
)
