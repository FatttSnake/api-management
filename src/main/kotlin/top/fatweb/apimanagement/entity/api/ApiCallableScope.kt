package top.fatweb.apimanagement.entity.api

/**
 * What an owner may call, as a value narrow enough to hand to a query
 *
 * The permission rule itself lives in one place - this is its resolved result, so a caller
 * that needs to filter interfaces by what an owner may reach passes it to the database
 * instead of reading every enabled interface and discarding most of them afterwards.
 *
 * [unrestricted] is not the same as an empty [codes]: the super admin sees every interface,
 * while an owner holding no codes still sees the open DEFAULT ones.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ApiCallableScope(
    /**
     * Whether the owner is the super admin and reaches every interface
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val unrestricted: Boolean,

    /**
     * Operation codes the owner holds, empty when it holds none
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val codes: Set<String>
)
