package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.param.system.api.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo

/**
 * API plugin service interface
 *
 * Manages the plugin registry (one row per plugin) and its per-interface
 * configuration (price / rate limit / billing), plus the hot-pluggable lifecycle:
 * [installPlugin] uploads and mounts a signed jar, [uninstallPlugin] unmounts it.
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
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun uninstallPlugin(pluginId: String)
}
