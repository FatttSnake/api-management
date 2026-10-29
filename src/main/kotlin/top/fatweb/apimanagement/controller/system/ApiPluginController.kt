package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateStatusParam
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginDatasourceTestParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateStatusParam
import top.fatweb.apimanagement.service.api.IPluginPlatformService
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
 */
@BaseController(path = ["/system/api"], name = "API 插件管理", description = "API 插件管理相关接口")
class ApiPluginController(private val pluginPlatformService: IPluginPlatformService) {
    /**
     * Get API plugin paging information
     *
     * @param apiPluginGetParam Get API plugin parameters
     * @return Response object includes API plugin paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取 API 插件列表")
    @GetMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:query')")
    fun getPlugin(@ProcessParam @Valid apiPluginGetParam: ApiPluginGetParam?): ResponseResult<PageVo<ApiPluginVo>> =
        ResponseResult.databaseSuccess(data = pluginPlatformService.getPluginPage(apiPluginGetParam))

    /**
     * Get one API plugin by its plugin ID
     *
     * @param pluginId Plugin ID
     * @return Response object includes API plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取 API 插件")
    @GetMapping("/plugin/{pluginId}/info")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:query')")
    fun getPluginInfo(@PathVariable pluginId: String): ResponseResult<ApiPluginVo> =
        ResponseResult.databaseSuccess(data = pluginPlatformService.getPlugin(pluginId))

    /**
     * Install a plugin from an uploaded jar
     *
     * @param file Plugin jar file
     * @return Response object includes installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "安装 API 插件")
    @PostMapping("/plugin/install")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:install')")
    fun install(@RequestPart("file") file: MultipartFile): ResponseResult<ApiPluginVo> = ResponseResult.databaseSuccess(
        ResponseCode.DATABASE_INSERT_SUCCESS,
        data =
        pluginPlatformService
            .install(
                jarBytes = file.bytes,
                jarName = file.originalFilename ?: "plugin.jar",
            ).toVo(),
    )

    /**
     * Update API plugin
     *
     * @param apiPluginUpdateParam Update API plugin parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改 API 插件")
    @PutMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:modify')")
    fun updatePlugin(
        @ProcessParam @Valid @RequestBody apiPluginUpdateParam: ApiPluginUpdateParam,
    ): ResponseResult<Unit> {
        pluginPlatformService.updatePlugin(apiPluginUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update API plugin enable status
     *
     * @param apiPluginUpdateStatusParam Update API plugin enable status parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改 API 插件启用状态")
    @PatchMapping("/plugin")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:status')")
    fun updatePluginStatus(
        @ProcessParam @Valid @RequestBody apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam,
    ): ResponseResult<Unit> {
        val plugin =
            apiPluginUpdateStatusParam.id
                ?.let(pluginPlatformService::getById)
                ?: throw PluginInstallException("Plugin not found")
        val pluginId = plugin.pluginId ?: throw PluginInstallException("Plugin has no pluginId")
        pluginPlatformService.setEnabled(pluginId, apiPluginUpdateStatusParam.enable == true)

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
        @RequestParam(name = "purgeData", defaultValue = "false") purgeData: Boolean,
    ): ResponseResult<Unit> {
        pluginPlatformService.uninstall(pluginId, purgeData)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Reload a plugin by its plugin ID
     *
     * @param pluginId Plugin ID
     * @return Response object includes reloaded plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "重新挂载 API 插件")
    @PostMapping("/plugin/{pluginId}/reload")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:reload')")
    fun reload(@PathVariable pluginId: String): ResponseResult<ApiPluginVo> = ResponseResult.databaseSuccess(
        ResponseCode.DATABASE_UPDATE_SUCCESS,
        data = pluginPlatformService.reload(pluginId).toVo(),
    )

    /**
     * Get the configuration of a plugin
     *
     * @param pluginId Plugin ID
     * @return Response object includes the declared config schema and its current values
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取 API 插件配置")
    @GetMapping("/plugin/{pluginId}/config")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:query')")
    fun getConfig(@PathVariable pluginId: String): ResponseResult<ApiPluginConfigVo> =
        ResponseResult.databaseSuccess(data = pluginPlatformService.getPluginConfig(pluginId))

    /**
     * Update the configuration of a plugin
     *
     * Changing a value that describes a datasource remounts the plugin once the transaction
     * commits, and so does any save at all while the plugin is down from a failed mount; a
     * failure shows up in the plugin's `loadError`, not in the response.
     *
     * @param apiPluginConfigUpdateParam Update API plugin config parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改 API 插件配置")
    @PutMapping("/plugin/config")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:modify')")
    fun updateConfig(
        @ProcessParam @Valid @RequestBody apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam,
    ): ResponseResult<Unit> {
        pluginPlatformService.updatePluginConfig(apiPluginConfigUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Test the connection of a plugin datasource
     *
     * @param pluginId Plugin ID
     * @param apiPluginDatasourceTestParam Test API plugin datasource parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "测试 API 插件数据源连接")
    @PostMapping("/plugin/{pluginId}/config/datasource/test")
    @PreAuthorize("hasAnyAuthority('system:plugin:config:modify')")
    fun testDatasource(
        @PathVariable pluginId: String,
        @ProcessParam @Valid @RequestBody apiPluginDatasourceTestParam: ApiPluginDatasourceTestParam,
    ): ResponseResult<Unit> {
        pluginPlatformService.testPluginDatasource(
            pluginId,
            apiPluginDatasourceTestParam.name.orEmpty(),
            // A value that is left out keeps its null, which is what says the stored one is
            // read: a form testing the configuration it was loaded with submits neither
            apiPluginDatasourceTestParam.values
                .orEmpty()
                .mapNotNull { value -> value.key?.let { it to value.value } }
                .toMap(),
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
     * @see ApiInterfaceVo
     */
    @Operation(summary = "获取 API 接口列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:query')")
    fun getInterface(
        @ProcessParam @Valid apiInterfaceGetParam: ApiInterfaceGetParam?,
    ): ResponseResult<PageVo<ApiGroupVo>> =
        ResponseResult.databaseSuccess(data = pluginPlatformService.getInterfacePage(apiInterfaceGetParam))

    /**
     * Update API interface configuration
     *
     * @param apiInterfaceUpdateParam Update API interface parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改 API 接口配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:modify')")
    fun updateInterface(
        @ProcessParam @Valid @RequestBody apiInterfaceUpdateParam: ApiInterfaceUpdateParam,
    ): ResponseResult<Unit> {
        pluginPlatformService.updateInterface(apiInterfaceUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update API interface enable status
     *
     * @param apiInterfaceUpdateStatusParam Update API interface enable status parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改 API 接口启用状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:interface:interface:status')")
    fun updateInterfaceStatus(
        @ProcessParam @Valid @RequestBody apiInterfaceUpdateStatusParam: ApiInterfaceUpdateStatusParam,
    ): ResponseResult<Unit> {
        pluginPlatformService.updateInterfaceStatus(apiInterfaceUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }
}
