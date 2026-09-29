package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiPluginSetting

/**
 * Plugin setting service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiPluginSetting
 */
interface IApiPluginSettingService : IService<ApiPluginSetting> {
    /**
     * Read a plugin setting
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @return Setting value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(pluginId: String, key: String): String?

    /**
     * List every setting of a plugin
     *
     * Values are returned exactly as they are stored, so a key the plugin declared as a
     * secret comes back as ciphertext. A row whose value is null is left out rather than
     * reported as an empty one: an empty string is a value in its own right - it is what a
     * cleared text setting is stored as - while a null, which nothing in the gateway writes,
     * means the key holds nothing, exactly as a missing row does.
     *
     * @param pluginId Plugin ID
     * @return Settings keyed by setting key; a key with no value is absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun listByPlugin(pluginId: String): Map<String, String>

    /**
     * Upsert a plugin setting
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @param value Setting value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun set(pluginId: String, key: String, value: String)

    /**
     * Delete one setting of a plugin
     *
     * How a key is cleared: the row goes, so the plugin reads the declared default again - or
     * nothing at all, when the key is a secret, which has no default to fall back to. A text
     * setting is the one kind that is never cleared this way, because a blank one is a value
     * it can hold.
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(pluginId: String, key: String)

    /**
     * Delete all settings of a plugin (on uninstall)
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteByPlugin(pluginId: String)
}
