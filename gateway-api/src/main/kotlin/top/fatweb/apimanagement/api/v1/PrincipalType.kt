package top.fatweb.apimanagement.api.v1

/**
 * Caller identity type.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class PrincipalType {
    /**
     * No authenticated identity.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    ANONYMOUS,

    /**
     * Platform account authenticated by a login token.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    USER,

    /**
     * Caller authenticated by an AccessKey.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    ACCESS_KEY,
}
