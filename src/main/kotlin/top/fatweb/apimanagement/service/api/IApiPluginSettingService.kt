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
     * Delete all settings of a plugin (on uninstall)
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteByPlugin(pluginId: String)
}
