package top.fatweb.apimanagement.api.v1

/**
 * Business failure a plugin may throw to produce a controlled error response.
 *
 * The gateway converts it to the standard business envelope. The HTTP status defaults to 200 so
 * callers keep receiving a business code.
 *
 * @property httpStatus HTTP status
 * @property code business code
 * @property data error payload
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ApiException @JvmOverloads constructor(
    val httpStatus: Int = 200,
    val code: Int = 400,
    message: String = "",
    val data: Any? = null
) : RuntimeException(message) {
    /**
     * Builds a failure.
     *
     * @param code business code
     * @param message failure message
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    constructor(code: Int, message: String) : this(200, code, message)

    /**
     * Builds a failure with a generic business code.
     *
     * @param message failure message
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    constructor(message: String) : this(200, 400, message)
}
