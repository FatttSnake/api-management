package top.fatweb.apimanagement.component.storage

/**
 * File storage routes
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object StorageRoutes {
    /**
     * Base path of the login-free download route
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val PUBLIC_STORAGE_PATH = "/public/storage"

    /**
     * Ant pattern matching [PUBLIC_STORAGE_PATH] and everything under it
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val PUBLIC_STORAGE_MATCHER = "$PUBLIC_STORAGE_PATH/**"
}
