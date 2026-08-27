package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.ApiInterface
import top.fatweb.apimanagement.entity.system.ApiPlugin
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiInterfaceVo
import top.fatweb.apimanagement.vo.system.ApiPluginVo

/**
 * API plugin service interface
 *
 * Manages both the plugin registry (one row per plugin) and its per-interface
 * configuration (price / rate limit / billing).
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
     * Get API interface in page
     *
     * @param apiInterfaceGetParam Get API interface parameters
     * @return PageVo<ApiInterfaceVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceGetParam
     * @see PageVo
     * @see ApiInterfaceVo
     */
    fun getInterfacePage(apiInterfaceGetParam: ApiInterfaceGetParam?): PageVo<ApiInterfaceVo>

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
     * List enabled API interfaces
     *
     * @return List<ApiInterface> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     */
    fun listEnabledInterfaces(): List<ApiInterface>

    /**
     * Register API controllers to the plugin registry
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun registerApis()
}
