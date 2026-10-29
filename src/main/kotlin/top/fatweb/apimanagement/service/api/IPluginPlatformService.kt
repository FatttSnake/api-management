package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateStatusParam
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo

/**
 * Module-based plugin platform.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IPluginPlatformService : IService<ApiPlugin> {
    /**
     * Installs or upgrades a plugin.
     *
     * The dependency modules are read from the module store, where
     * [IPluginModuleService.stage] put them before the plugin was uploaded.
     *
     * @param jarBytes plugin jar
     * @param jarName original jar name
     * @return installed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun install(jarBytes: ByteArray, jarName: String): ApiPlugin

    /**
     * Uninstalls a plugin.
     *
     * @param pluginId plugin ID
     * @param purgeData whether to delete plugin-owned data
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun uninstall(pluginId: String, purgeData: Boolean)

    /**
     * Enables or disables a plugin.
     *
     * @param pluginId plugin ID
     * @param enabled desired state
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setEnabled(pluginId: String, enabled: Boolean)

    /**
     * Reloads a plugin from its stored jar.
     *
     * @param pluginId plugin ID
     * @return refreshed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun reload(pluginId: String): ApiPlugin

    /**
     * Finds a plugin.
     *
     * @param pluginId plugin ID
     * @return plugin or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(pluginId: String): ApiPlugin?

    /**
     * Gets a plugin page.
     *
     * @param param query parameters
     * @return plugin page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPluginPage(param: ApiPluginGetParam?): PageVo<ApiPluginVo>

    /**
     * Gets one plugin for the management API.
     *
     * @param pluginId plugin ID
     * @return plugin value object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPlugin(pluginId: String): ApiPluginVo

    /**
     * Updates plugin-level governance defaults.
     *
     * @param param update parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updatePlugin(param: ApiPluginUpdateParam)

    /**
     * Gets an interface page grouped by plugin.
     *
     * @param param query parameters
     * @return interface page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getInterfacePage(param: ApiInterfaceGetParam?): PageVo<ApiGroupVo>

    /**
     * Updates interface governance.
     *
     * @param param update parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateInterface(param: ApiInterfaceUpdateParam)

    /**
     * Updates an interface enable flag.
     *
     * @param param update parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateInterfaceStatus(param: ApiInterfaceUpdateStatusParam)

    /**
     * Gets a plugin configuration form.
     *
     * @param pluginId plugin ID
     * @return configuration value object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPluginConfig(pluginId: String): ApiPluginConfigVo

    /**
     * Saves plugin configuration groups.
     *
     * @param param update parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updatePluginConfig(param: ApiPluginConfigUpdateParam)

    /**
     * Tests one plugin datasource.
     *
     * @param pluginId plugin ID
     * @param name datasource name
     * @param values submitted values
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun testPluginDatasource(pluginId: String, name: String, values: Map<String, String?>)

    /**
     * Loads every enabled plugin from disk.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadInstalled()
}
