package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.*
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiInterfaceVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo

/**
 * API plugin management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginService
 */
@BaseController(path = ["/system/api"], name = "API 插件管理", description = "API 插件管理相关接口")
class ApiPluginController(
    private val apiPluginService: IApiPluginService
) {
    /**
     * Get API plugin paging information
     *
     * @param apiPluginGetParam Get API plugin parameters
     * @return Response object includes API plugin paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiPluginVo
     */
    @Operation(summary = "获取 API 插件列表")
    @GetMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:query')")
    fun getPlugin(@ProcessParam @Valid apiPluginGetParam: ApiPluginGetParam?): ResponseResult<PageVo<ApiPluginVo>> =
        ResponseResult.databaseSuccess(data = apiPluginService.getPluginPage(apiPluginGetParam))

    /**
     * Get one API plugin by its plugin ID
     *
     * The variable sits after the plugin ID rather than directly under the plugin prefix
     * because `key` and `install` are literal routes there, and a plugin ID is allowed to
     * be either of those words.
     *
     * @param pluginId Plugin ID
     * @return Response object includes API plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiPluginVo
     */
    @Operation(summary = "获取 API 插件")
    @GetMapping("/plugin/{pluginId}/info")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:query')")
    fun getPluginInfo(@PathVariable pluginId: String): ResponseResult<ApiPluginVo> =
        ResponseResult.databaseSuccess(data = apiPluginService.getPlugin(pluginId))

    /**
     * Install a plugin from an uploaded jar
     *
     * @param file Plugin jar file
     * @return Response object includes installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MultipartFile
     * @see ResponseResult
     * @see ApiPluginVo
     */
    @Operation(summary = "安装 API 插件")
    @PostMapping("/plugin/install")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:install')")
    fun install(@RequestPart("file") file: MultipartFile): ResponseResult<ApiPluginVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_INSERT_SUCCESS,
            data = apiPluginService.installPlugin(file.bytes, file.originalFilename ?: "plugin.jar")
        )

    /**
     * Update API plugin
     *
     * @param apiPluginUpdateParam Update API plugin parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 插件")
    @PutMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:modify')")
    fun updatePlugin(@ProcessParam @Valid @RequestBody apiPluginUpdateParam: ApiPluginUpdateParam): ResponseResult<Unit> {
        apiPluginService.updatePlugin(apiPluginUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update API plugin enable status
     *
     * @param apiPluginUpdateStatusParam Update API plugin enable status parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 插件启用状态")
    @PatchMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:status')")
    fun updatePluginStatus(@ProcessParam @Valid @RequestBody apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam): ResponseResult<Unit> {
        apiPluginService.updatePluginStatus(apiPluginUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Uninstall a plugin by its plugin ID
     *
     * @param pluginId Plugin ID
     * @param purgeData Whether everything the plugin owns should be deleted with it - its
     *        settings, its datasource configuration and the files it stored. Defaults to
     *        false, because plugin data cannot be recovered while a reinstall can always
     *        be purged afterwards
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "卸载 API 插件")
    @DeleteMapping("/plugin/{pluginId}")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:uninstall')")
    fun uninstall(
        @PathVariable pluginId: String,
        @RequestParam(name = "purgeData", defaultValue = "false") purgeData: Boolean
    ): ResponseResult<Unit> {
        apiPluginService.uninstallPlugin(pluginId, purgeData)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Reload a plugin by its plugin ID
     *
     * Remounts the installed plugin from its stored jar, so one that has been replaced
     * under it takes effect without restarting the gateway. The plugin is briefly
     * unmounted while the swap happens.
     *
     * A configuration change does not need this: saving one that alters a datasource
     * remounts the plugin on its own, because the gateway is the one that builds the
     * datasource from it.
     *
     * @param pluginId Plugin ID
     * @return Response object includes reloaded plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiPluginVo
     */
    @Operation(summary = "重新挂载 API 插件")
    @PostMapping("/plugin/{pluginId}/reload")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:reload')")
    fun reload(@PathVariable pluginId: String): ResponseResult<ApiPluginVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_UPDATE_SUCCESS,
            data = apiPluginService.reloadPlugin(pluginId)
        )

    /**
     * Get the configuration of a plugin
     *
     * @param pluginId Plugin ID
     * @return Response object includes the declared config schema and its current values
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiPluginConfigVo
     */
    @Operation(summary = "获取 API 插件配置")
    @GetMapping("/plugin/{pluginId}/config")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:query')")
    fun getConfig(@PathVariable pluginId: String): ResponseResult<ApiPluginConfigVo> =
        ResponseResult.databaseSuccess(data = apiPluginService.getPluginConfig(pluginId))

    /**
     * Update the configuration of a plugin
     *
     * Saved a group at a time, and a group is also what the submission answers for: the
     * required fields of the groups it names have to hold a value once it is written, while a
     * group left out is one nothing was decided about.
     *
     * A value left out keeps the stored one, a blank one clears the key so the declared
     * default applies again - except for a text field, where a blank is a value it can hold -
     * and any other value is stored as submitted.
     *
     * An ordinary value takes effect on the plugin's next read, so nothing is remounted for
     * it. A value that describes a datasource is different, because the gateway is what
     * builds the connection from it: changing one of those remounts the plugin, so the
     * administrator does not have to know that a remount is what it takes. The response says
     * nothing about how that went, because the remount happens after this transaction has
     * committed - a failure is reported in the plugin's `loadError`, where the plugin list
     * shows it.
     *
     * @param apiPluginConfigUpdateParam Update API plugin config parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 插件配置")
    @PutMapping("/plugin/config")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:modify')")
    fun updateConfig(@ProcessParam @Valid @RequestBody apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam): ResponseResult<Unit> {
        apiPluginService.updatePluginConfig(apiPluginConfigUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Test the connection of a plugin datasource
     *
     * A datasource is a sub-resource of the plugin's configuration rather than a resource
     * of its own: it is declared by the plugin, described entirely by ordinary config
     * values, and dies with the plugin. Its values are read and written through the config
     * endpoints above; what this one adds is the answer a form cannot give - whether the
     * server described is actually reachable.
     *
     * Asked for rather than checked while saving, in both directions: a configuration whose
     * server is not up yet is still worth storing, and a test can be run against values
     * that have not been saved yet. A `values` entry that is left out, or sent with no value,
     * is read from what is stored.
     *
     * Only a MySQL datasource can be tested: a SQLite one is a file the gateway owns and
     * supplies itself, so there is no connection of the administrator's to check.
     *
     * This makes the gateway open a connection to an address the caller names, which is a
     * capability worth naming: it is why the endpoint sits behind `modify` rather than
     * `query`. It is no more than saving a datasource already did, and the caller is an
     * administrator who could point a plugin anywhere regardless.
     *
     * @param pluginId Plugin ID
     * @param apiPluginDatasourceTestParam Test API plugin datasource parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginDatasourceTestParam
     * @see ResponseResult
     */
    @Operation(summary = "测试 API 插件数据源连接")
    @PostMapping("/plugin/{pluginId}/config/datasource/test")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:modify')")
    fun testDatasource(
        @PathVariable pluginId: String,
        @ProcessParam @Valid @RequestBody apiPluginDatasourceTestParam: ApiPluginDatasourceTestParam
    ): ResponseResult<Unit> {
        apiPluginService.testPluginDatasource(
            pluginId,
            apiPluginDatasourceTestParam.name.orEmpty(),
            // A value that is left out keeps its null, which is what says the stored one is
            // read: a form testing the configuration it was loaded with submits neither
            apiPluginDatasourceTestParam.values.orEmpty()
                .mapNotNull { value -> value.key?.let { it to value.value } }
                .toMap()
        )

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_SELECT_SUCCESS)
    }

    /**
     * Get API interface paging information
     *
     * @param apiInterfaceGetParam Get API interface parameters
     * @return Response object includes API interface paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiInterfaceVo
     */
    @Operation(summary = "获取 API 接口列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:query')")
    fun getInterface(@ProcessParam @Valid apiInterfaceGetParam: ApiInterfaceGetParam?): ResponseResult<PageVo<ApiGroupVo>> =
        ResponseResult.databaseSuccess(data = apiPluginService.getInterfacePage(apiInterfaceGetParam))

    /**
     * Update API interface configuration
     *
     * @param apiInterfaceUpdateParam Update API interface parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 接口配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:modify')")
    fun updateInterface(@ProcessParam @Valid @RequestBody apiInterfaceUpdateParam: ApiInterfaceUpdateParam): ResponseResult<Unit> {
        apiPluginService.updateInterface(apiInterfaceUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update API interface enable status
     *
     * @param apiInterfaceUpdateStatusParam Update API interface enable status parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API 接口启用状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:status')")
    fun updateInterfaceStatus(@ProcessParam @Valid @RequestBody apiInterfaceUpdateStatusParam: ApiInterfaceUpdateStatusParam): ResponseResult<Unit> {
        apiPluginService.updateInterfaceStatus(apiInterfaceUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }
}
