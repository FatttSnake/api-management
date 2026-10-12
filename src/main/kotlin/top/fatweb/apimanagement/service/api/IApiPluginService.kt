package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.param.system.api.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo

/**
 * API plugin service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiPluginService : IService<ApiPlugin> {
    /**
     * Get API plugin in page
     *
     * @param apiPluginGetParam Get API plugin parameters
     * @return PageVo<ApiPluginVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPluginPage(apiPluginGetParam: ApiPluginGetParam?): PageVo<ApiPluginVo>

    /**
     * Get one API plugin by its plugin ID
     *
     * @param pluginId Plugin ID
     * @return ApiPluginVo object
     * @throws NoRecordFoundException when no such plugin is installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPlugin(pluginId: String): ApiPluginVo

    /**
     * Update API plugin
     *
     * @param apiPluginUpdateParam Update API plugin parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updatePlugin(apiPluginUpdateParam: ApiPluginUpdateParam)

    /**
     * Update API plugin enable status
     *
     * @param apiPluginUpdateStatusParam Update API plugin enable status parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updatePluginStatus(apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam)

    /**
     * Get API interface in page
     *
     * @param apiInterfaceGetParam Get API interface parameters
     * @return PageVo<ApiGroupVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getInterfacePage(apiInterfaceGetParam: ApiInterfaceGetParam?): PageVo<ApiGroupVo>

    /**
     * Update API interface configuration
     *
     * @param apiInterfaceUpdateParam Update API interface parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateInterface(apiInterfaceUpdateParam: ApiInterfaceUpdateParam)

    /**
     * Update API interface enable status
     *
     * @param apiInterfaceUpdateStatusParam Update API interface enable status parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateInterfaceStatus(apiInterfaceUpdateStatusParam: ApiInterfaceUpdateStatusParam)

    /**
     * Get API plugin by plugin ID from in-memory registry
     *
     * @param pluginId Plugin ID
     * @return ApiPlugin object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getByPluginId(pluginId: String): ApiPlugin?

    /**
     * Get API interface by code from in-memory registry
     *
     * @param code API scoping code
     * @return ApiInterface object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getByCode(code: String): ApiInterface?

    /**
     * Resolve the effective access mode for an interface (interface override or
     * inherited from the owning plugin, defaulting to RESTRICTED)
     *
     * @param api API interface
     * @return Effective access mode
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    fun resolveAccessMode(api: ApiInterface): ApiInterface.AccessMode

    /**
     * Install a plugin from an uploaded jar
     *
     * A plugin whose own `onStart` throws is still installed and registered, serving nothing with
     * the reason in `loadError`.
     *
     * @param jarBytes Plugin jar content
     * @param jarName Original jar file name
     * @return ApiPluginVo object of the installed plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun installPlugin(jarBytes: ByteArray, jarName: String): ApiPluginVo

    /**
     * Uninstall a plugin by its plugin ID
     *
     * A purge runs the plugin's own `onPurge` while its datasources are still open.
     *
     * @param pluginId Plugin ID
     * @param purgeData Whether everything the plugin owns should be deleted with it -
     *        its settings, its datasource configuration and the files it stored. False
     *        keeps them, so reinstalling the same plugin ID picks up where it left off
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun uninstallPlugin(pluginId: String, purgeData: Boolean)

    /**
     * Re-mount an installed plugin from its stored jar
     *
     * The replacement is built in full before the running one is taken down, so a jar that
     * cannot be built at all - a bad signature, a descriptor that is refused, a class that will
     * not load - leaves the plugin serving what it was. Once the build succeeds the running
     * instance is torn down and replaced, so a plugin whose own `onStart` then throws ends up
     * registered and serving nothing, with the reason in `loadError`, exactly as a fresh
     * install of it would.
     *
     * @param pluginId Plugin ID
     * @return ApiPluginVo object of the reloaded plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun reloadPlugin(pluginId: String): ApiPluginVo

    /**
     * Verify that a plugin's datasource can be connected to
     *
     * @param pluginId Plugin ID
     * @param name Datasource name
     * @param values Config values to try, keyed by config key; a key left out, or sent as
     *        null, is read from what is stored
     * @throws top.fatweb.apimanagement.exception.PluginDatasourceException when the
     *         datasource is undeclared, not configurable, not configured, described by
     *         values that do not belong to it, or unreachable
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun testPluginDatasource(pluginId: String, name: String, values: Map<String, String?>)

    /**
     * Get the configuration of a plugin
     *
     * @param pluginId Plugin ID
     * @return ApiPluginConfigVo object, whose schema is null when the plugin declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPluginConfig(pluginId: String): ApiPluginConfigVo

    /**
     * Save the configuration of a plugin
     *
     * Saved one group at a time; the required fields of the groups a submission names are what it
     * is checked against. An ordinary value takes effect on the plugin's next read, while a
     * change to a value a datasource is composed from remounts the plugin once the transaction
     * commits - and so does any save at all while the plugin is down from a failed mount, since
     * what stopped it need not be one of those values.
     *
     * @param apiPluginConfigUpdateParam Update API plugin config parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updatePluginConfig(apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam)
}
