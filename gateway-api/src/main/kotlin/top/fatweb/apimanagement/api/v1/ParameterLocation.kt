package top.fatweb.apimanagement.api.v1

/**
 * Where an operation parameter is read from.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class ParameterLocation {
    /**
     * Path parameter.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    PATH,

    /**
     * Query parameter.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    QUERY,

    /**
     * Request header.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    HEADER,

    /**
     * Cookie.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    COOKIE
}
