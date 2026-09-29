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
 * Manages the plugin registry (one row per plugin) and its per-interface
 * configuration (price / rate limit / billing), plus the hot-pluggable lifecycle:
 * [installPlugin] uploads and mounts a signed jar, [uninstallPlugin] unmounts it and
 * [reloadPlugin] remounts it. Also owns the administrator-facing configuration of a
 * plugin ([getPluginConfig] / [updatePluginConfig]), whose declared schema comes from
 * the plugin's own jar.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiPlugin
 */
interface IApiPluginService : IService<ApiPlugin> {
    /**
     * Get API plugin in page
     *
     * @param apiPluginGetParam Get API plugin parameters
     * @return PageVo<ApiPluginVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginGetParam
     * @see PageVo
     * @see ApiPluginVo
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
     * @see ApiPluginVo
     */
    fun getPlugin(pluginId: String): ApiPluginVo

    /**
     * Update API plugin
     *
     * @param apiPluginUpdateParam Update API plugin parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginUpdateParam
     */
    fun updatePlugin(apiPluginUpdateParam: ApiPluginUpdateParam)

    /**
     * Update API plugin enable status
     *
     * @param apiPluginUpdateStatusParam Update API plugin enable status parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginUpdateStatusParam
     */
    fun updatePluginStatus(apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam)

    /**
     * Get API interface in page
     *
     * @param apiInterfaceGetParam Get API interface parameters
     * @return PageVo<ApiGroupVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceGetParam
     * @see PageVo
     * @see ApiGroupVo
     */
    fun getInterfacePage(apiInterfaceGetParam: ApiInterfaceGetParam?): PageVo<ApiGroupVo>

    /**
     * Update API interface configuration
     *
     * @param apiInterfaceUpdateParam Update API interface parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceUpdateParam
     */
    fun updateInterface(apiInterfaceUpdateParam: ApiInterfaceUpdateParam)

    /**
     * Update API interface enable status
     *
     * @param apiInterfaceUpdateStatusParam Update API interface enable status parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceUpdateStatusParam
     */
    fun updateInterfaceStatus(apiInterfaceUpdateStatusParam: ApiInterfaceUpdateStatusParam)

    /**
     * Get API plugin by plugin ID from in-memory registry
     *
     * @param pluginId Plugin ID
     * @return ApiPlugin object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPlugin
     */
    fun getByPluginId(pluginId: String): ApiPlugin?

    /**
     * Get API interface by code from in-memory registry
     *
     * @param code API scoping code
     * @return ApiInterface object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     */
    fun getByCode(code: String): ApiInterface?

    /**
     * List enabled API interfaces
     *
     * @return List<ApiInterface> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     */
    fun listEnabledInterfaces(): List<ApiInterface>

    /**
     * Resolve the effective access mode for an interface (interface override or
     * inherited from the owning plugin, defaulting to RESTRICTED)
     *
     * @param api API interface
     * @return Effective access mode
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     * @see ApiInterface.AccessMode
     */
    fun resolveAccessMode(api: ApiInterface): ApiInterface.AccessMode

    /**
     * Install a plugin from an uploaded jar
     *
     * Verifies the signature and trust, then mounts the plugin: registers its
     * request mappings, upserts database rows and the permission tree. Upgrades
     * (same plugin ID) are only allowed when the new version code is higher.
     *
     * @param jarBytes Plugin jar content
     * @param jarName Original jar file name
     * @return ApiPluginVo object of the installed plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     * @see ApiPluginVo
     */
    fun installPlugin(jarBytes: ByteArray, jarName: String): ApiPluginVo

    /**
     * Uninstall a plugin by its plugin ID
     *
     * Unregisters its request mappings, closes its class loader / child context,
     * soft-deletes its database rows and cleans up the permission tree.
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
     * Restarts the plugin's class loader, child context and request mappings without
     * touching its version, its database rows, its permission tree, its configuration
     * or its stored files - so a jar that has been replaced under it is picked up
     * without restarting the gateway.
     *
     * A configuration change does not need this: saving one that alters a datasource
     * remounts the plugin on its own, because the gateway is the one that builds the
     * datasource from it.
     *
     * The plugin is briefly unmounted while the swap happens, and an upgrade path is not
     * reused: that one deletes and rebuilds the registration, which would reset the
     * interface configuration the administrator has tuned.
     *
     * @param pluginId Plugin ID
     * @return ApiPluginVo object of the reloaded plugin
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginVo
     */
    fun reloadPlugin(pluginId: String): ApiPluginVo

    /**
     * Verify that a plugin's datasource can be connected to
     *
     * Asked for rather than done while saving, so a configuration is worth storing before
     * its server is reachable and is tried out when the administrator wants it tried.
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
     * @see ApiPluginConfigVo
     */
    fun getPluginConfig(pluginId: String): ApiPluginConfigVo

    /**
     * Save the configuration of a plugin
     *
     * Saved one group at a time, since that is how the console renders it: a submission names
     * the groups it is deciding about, and the required fields those groups declare are what
     * it is checked against.
     *
     * @param apiPluginConfigUpdateParam Update API plugin config parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigUpdateParam
     */
    fun updatePluginConfig(apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam)
}
