package top.fatweb.apimanagement.api.v1

/**
 * Plugin-scoped settings.
 *
 * Keys declared by the plugin configuration schema are administrator-owned and cannot be
 * written through [save].
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface Settings {
    /**
     * Reads a setting.
     *
     * @param key setting key
     * @return value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(key: String): String?

    /**
     * Reads a secret setting as plaintext.
     *
     * @param key setting key
     * @return decrypted value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getSecret(key: String): String?

    /**
     * Persists a plugin-owned setting.
     *
     * @param key setting key
     * @param value setting value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun save(key: String, value: String)

    /**
     * Returns non-secret settings.
     *
     * @return immutable setting map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun all(): Map<String, String>
}
