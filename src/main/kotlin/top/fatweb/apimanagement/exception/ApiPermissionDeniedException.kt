package top.fatweb.apimanagement.exception

/**
 * API permission denied exception
 *
 * Thrown when an account (JWT) caller invokes a RESTRICTED API interface that
 * they have not been granted via their operations (the api:* operation code).
 * Only governs the account/password path — an AccessKey is still gated by its
 * own permissions snapshot in [ApiKeyPermissionDeniedException].
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class ApiPermissionDeniedException : RuntimeException("API permission denied")
