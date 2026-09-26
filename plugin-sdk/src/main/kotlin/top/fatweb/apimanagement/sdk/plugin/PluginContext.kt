package top.fatweb.apimanagement.sdk.plugin

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Plugin context
 *
 * The narrow, gateway-sanctioned data interaction channel for a plugin. A plugin
 * must never access the gateway's own datasource / MyBatis mappers; it reads
 * gateway data only through this interface and writes its own data via
 * [datasources] (isolated, admin-configured databases dedicated to this plugin)
 * and [storage] (a file area isolated to this plugin).
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
     * The plugin's own isolated datasources, by the name each was declared under
     *
     * A plugin is never given the gateway's datasource: it only ever gets one because it
     * declared a `datasources` entry, with a `name` and a `dbType`, in its own
     * `META-INF/plugin-config.json`. The connection itself is declared as ordinary config
     * fields, which the gateway reads when it builds the datasource.
     *
     * What a declaration means depends on the dialect. A `SQLITE` one is supplied by the
     * gateway the moment it is declared, because there is nothing an administrator could
     * decide about it. A `MYSQL` one is configured by an administrator and is **absent**
     * from this map until they do - as is any datasource whose connection information is
     * incomplete. A plugin that declares one as `required` still mounts meanwhile: it is
     * up to the plugin to report the missing database clearly instead of failing at mount
     * time.
     *
     * Each datasource is built while mounting, and saving a change to a datasource's
     * configuration remounts the plugin on its own, so a plugin only has to expect its
     * `onStop` and `onStart` to run again - any state it holds in memory is lost with them.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see DataSource
     */
    val datasources: Map<String, DataSource>

    /**
     * The plugin's own isolated file storage
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginStorage
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
     * Two kinds of key are readable through this one method:
     *
     * - A key declared by the plugin's own config schema - see the `PluginConfig`
     *   section of the plugin development guide - is an administrator-owned
     *   configuration value. When the administrator has not set one, the schema's
     *   declared default is returned, so a plugin never repeats its defaults in code.
     *   A key declared as a secret is returned as plaintext.
     * - Any other key is the plugin's own runtime state, written through [saveSetting].
     *
     * Configuration changes are visible immediately: the value is read per call, so
     * nothing has to be remounted for a new value to take effect.
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
     * Only keys the plugin owns can be written. A key declared by the plugin's own
     * config schema belongs to the administrator, and writing it raises rather than
     * silently diverging from what the administrator sees and edits.
     *
     * @param key Setting key
     * @param value Setting value
     * @throws IllegalArgumentException when the key is declared by the plugin's config
     *         schema and is therefore administrator-owned
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
