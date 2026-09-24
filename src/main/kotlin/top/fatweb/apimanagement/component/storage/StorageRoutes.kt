package top.fatweb.apimanagement.component.storage

/**
 * File storage routes
 *
 * The login-free download route is referenced by the controller that serves it, the
 * security matcher that admits it and the token filter that must skip it, so it
 * lives here to keep those three from drifting apart.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object StorageRoutes {
    /**
     * Base path of the login-free download route
     */
    const val PUBLIC_STORAGE_PATH = "/public/storage"

    /**
     * Ant pattern matching [PUBLIC_STORAGE_PATH] and everything under it
     */
    const val PUBLIC_STORAGE_MATCHER = "$PUBLIC_STORAGE_PATH/**"
}
