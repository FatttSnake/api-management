package top.fatweb.apimanagement.api.v1

/**
 * One plugin audit event.
 *
 * @property action action identifier
 * @property target affected object
 * @property detail human-readable detail
 * @property attributes structured attributes
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class AuditEvent @JvmOverloads constructor(
    val action: String,
    val target: String = "",
    val detail: String = "",
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(action.isNotBlank()) { "action must not be blank" }
    }
}
