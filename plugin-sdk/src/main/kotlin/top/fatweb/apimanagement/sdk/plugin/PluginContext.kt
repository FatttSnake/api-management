package top.fatweb.apimanagement.sdk.plugin

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Plugin context
 *
 * A plugin must never access the gateway's own datasource or MyBatis mappers; it reaches
 * gateway data only through this interface.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginContext {
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: String

    /**
     * Class loader of the plugin
     *
     * Resource lookups see the plugin jar and the libraries it bundles; class lookups still
     * resolve gateway-provided classes. Use it instead of the thread context class loader when
     * loading resources from a thread the plugin creates itself.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val classLoader: ClassLoader

    /**
     * The plugin's own isolated datasources, by the name each was declared under
     *
     * A `SQLITE` datasource appears the moment it is declared; a `MYSQL` one only once an
     * administrator configures it, and is absent until then. A save that changes a datasource's
     * configuration remounts the plugin.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val datasources: Map<String, DataSource>

    /**
     * The plugin's own isolated file storage
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val storage: PluginStorage

    /**
     * ID of the user currently invoking the API, or null when unauthenticated
     *
     * @return User ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun currentUserId(): Long?

    /**
     * ID of the API key currently invoking the API, or null when the caller is an
     * account (LoginUser) rather than a key
     *
     * @return API key ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun currentAccessKeyId(): Long?

    /**
     * Get the account balance of a user
     *
     * @param userId User ID
     * @return Balance
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBalance(userId: Long): BigDecimal

    /**
     * Get the runtime configuration of a registered interface
     *
     * @param code Interface code, e.g. "api:echo:v1:ping"
     * @return Interface info, or null when not registered
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getInterfaceInfo(code: String): PluginInterfaceInfo?

    /**
     * Read a plugin-scoped setting
     *
     * A key declared as a secret is returned as plaintext, and a configuration change takes
     * effect immediately - nothing is remounted for a new value.
     *
     * @param key Setting key
     * @return Setting value, the schema default, or null when neither exists
     * @throws IllegalStateException when a secret cannot be decrypted, which happens
     *         after the gateway's token secret has been rotated
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getSetting(key: String): String?

    /**
     * Persist a plugin-scoped setting
     *
     * @param key Setting key, at most 100 characters - the same limit administrator-owned
     *        keys are held to, since both are stored in the same column
     * @param value Setting value
     * @throws IllegalArgumentException when the key is declared by the plugin's config
     *         schema and is therefore administrator-owned, or is longer than the limit
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveSetting(key: String, value: String)
}

/**
 * Runtime configuration snapshot of a registered interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginInterfaceInfo(
    /**
     * Interface code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val code: String,

    /**
     * Interface display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val name: String?,

    /**
     * Whether the interface is enabled
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val enable: Boolean,

    /**
     * Price per call; null means free / inherited
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val price: BigDecimal?,

    /**
     * Billing mode: FREE / SUCCESS_ONLY / ALWAYS
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val billingMode: String?,

    /**
     * Whether an API key is required
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val needKey: Boolean,

    /**
     * Rate limit per minute; null means inherited / unlimited
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val rateLimit: Int?,

    /**
     * Access mode: DEFAULT / RESTRICTED
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val accessMode: String?
)
