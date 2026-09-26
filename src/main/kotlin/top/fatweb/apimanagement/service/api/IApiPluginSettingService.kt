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
     * secret comes back as ciphertext.
     *
     * @param pluginId Plugin ID
     * @return Settings keyed by setting key
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
     * Used to clear a secret, which has no default to fall back to and is therefore
     * cleared by removing the row rather than by storing an empty value.
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
