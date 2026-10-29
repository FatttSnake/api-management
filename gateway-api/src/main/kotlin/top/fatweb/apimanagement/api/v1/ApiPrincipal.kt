package top.fatweb.apimanagement.api.v1

/**
 * Caller identity for one request.
 *
 * @property type principal type
 * @property userId platform user ID, or null for an anonymous caller
 * @property accessKeyId API key ID, or null when the caller is not using a key
 * @property name display name
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ApiPrincipal @JvmOverloads constructor(
    val type: PrincipalType = PrincipalType.ANONYMOUS,
    val userId: Long? = null,
    val accessKeyId: Long? = null,
    val name: String = "",
) {
    companion object {
        /**
         * Returns the anonymous principal.
         *
         * @return anonymous principal
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun anonymous(): ApiPrincipal = ApiPrincipal()
    }
}
