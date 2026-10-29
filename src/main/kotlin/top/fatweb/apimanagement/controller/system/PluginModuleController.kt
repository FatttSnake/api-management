package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.PluginModuleGetParam
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IPluginModuleService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleVo
import java.time.Duration

/**
 * Plugin dependency module management controller.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@BaseController(
    path = ["/system/api/plugin/module"],
    name = "API 插件依赖模块",
    description = "API 插件依赖模块相关接口",
)
class PluginModuleController(
    private val pluginModuleService: IPluginModuleService,
    private val serverProperties: ServerProperties,
) {
    /**
     * Lists installed modules.
     *
     * @return module page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取 API 插件依赖模块列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:query')")
    fun getModules(@ProcessParam @Valid param: PluginModuleGetParam?): ResponseResult<PageVo<PluginModuleVo>> =
        ResponseResult.databaseSuccess(data = pluginModuleService.getPage(param))

    /**
     * Uploads a dependency module before its plugin.
     *
     * @param file module jar
     * @return module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "上传 API 插件依赖模块")
    @PostMapping("/install")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:install')")
    fun install(@RequestPart("file") file: MultipartFile): ResponseResult<String> = ResponseResult.databaseSuccess(
        ResponseCode.DATABASE_INSERT_SUCCESS,
        data = pluginModuleService.stage(file.bytes),
    )

    /**
     * Deletes an unreferenced module.
     *
     * @param sha256 module digest
     * @return response
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "删除 API 插件依赖模块")
    @DeleteMapping("/{sha256}")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:uninstall')")
    fun delete(@PathVariable sha256: String): ResponseResult<Unit> {
        pluginModuleService.delete(sha256)
        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Collects unreferenced modules.
     *
     * @return number of deleted module files
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "回收 API 插件依赖模块")
    @PostMapping("/gc")
    @PreAuthorize("hasAnyAuthority('system:plugin:plugin:install')")
    fun gc(): ResponseResult<Int> = ResponseResult.databaseSuccess(
        ResponseCode.DATABASE_DELETE_SUCCESS,
        data =
        pluginModuleService.gc(
            Duration.ofHours(serverProperties.storage.pluginModuleGcGraceHours),
        ),
    )
}
