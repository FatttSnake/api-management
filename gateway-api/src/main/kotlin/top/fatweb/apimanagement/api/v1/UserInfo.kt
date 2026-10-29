package top.fatweb.apimanagement.api.v1

/**
 * Read-only platform user snapshot.
 *
 * @property id user ID
 * @property username login name
 * @property nickname display name
 * @property email email address
 * @property enabled whether the account is enabled
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class UserInfo @JvmOverloads constructor(
    val id: Long,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val enabled: Boolean = true,
)
