package top.fatweb.apimanagement.sdk.plugin

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Plugin context
 *
 * The narrow, gateway-sanctioned data interaction channel for a plugin. A plugin
 * must never access the gateway's own datasource / MyBatis mappers; it reads
 * gateway data only through this interface and writes its own data via
 * [datasource] (an isolated, admin-configured datasource dedicated to this plugin).
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginLifecycle
 */
interface PluginContext {
    /**
     * Plugin ID
     */
    val pluginId: String

    /**
     * The plugin's own isolated datasource, or null when the administrator has not
     * configured one for this plugin
     */
    val datasource: DataSource?

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
     * @return Balance, or null when the account does not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBalance(userId: Long): BigDecimal?

    /**
     * Get the runtime configuration of a registered interface
     *
     * @param code Interface code, e.g. "api:echo:v1:ping"
     * @return Interface info, or null when not registered
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginInterfaceInfo
     */
    fun getInterfaceInfo(code: String): PluginInterfaceInfo?

    /**
     * Read a plugin-scoped setting
     *
     * @param key Setting key
     * @return Setting value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getSetting(key: String): String?

    /**
     * Persist a plugin-scoped setting
     *
     * @param key Setting key
     * @param value Setting value
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
     */
    val code: String,

    /**
     * Interface display name
     */
    val name: String?,

    /**
     * Whether the interface is enabled
     */
    val enable: Boolean,

    /**
     * Price per call; null means free / inherited
     */
    val price: BigDecimal?,

    /**
     * Billing mode: FREE / SUCCESS_ONLY / ALWAYS
     */
    val billingMode: String?,

    /**
     * Whether an API key is required
     */
    val needKey: Boolean,

    /**
     * Rate limit per minute; null means inherited / unlimited
     */
    val rateLimit: Int?,

    /**
     * Access mode: DEFAULT / RESTRICTED
     */
    val accessMode: String?
)
