package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyAddParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyUpdateStatusParam
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiPluginTrustKeyVo

/**
 * API plugin trust key management controller
 *
 * Manages the public keys trusted to sign plugin jars. Only signers whose key is
 * present here and enabled can have their plugins installed.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginTrustKeyService
 */
@BaseController(
    path = ["/system/api/plugin/key"],
    name = "插件信任密钥",
    description = "API 插件签名信任密钥管理相关接口"
)
class ApiTrustKeyController(
    private val apiPluginTrustKeyService: IApiPluginTrustKeyService
) {
    /**
     * Get plugin trust key list
     *
     * @param apiPluginTrustKeyGetParam Get API plugin trust key parameters
     * @return Response object includes plugin trust key list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKeyGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiPluginTrustKeyVo
     */
    @Operation(summary = "获取插件信任密钥列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:plugin:key:query')")
    fun get(apiPluginTrustKeyGetParam: ApiPluginTrustKeyGetParam): ResponseResult<PageVo<ApiPluginTrustKeyVo>> =
        ResponseResult.databaseSuccess(data = apiPluginTrustKeyService.get(apiPluginTrustKeyGetParam))

    /**
     * Add plugin trust key
     *
     * @param apiPluginTrustKeyAddParam Add plugin trust key parameters
     * @return Response object includes added plugin trust key information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKeyAddParam
     * @see ResponseResult
     * @see ApiPluginTrustKeyVo
     */
    @Operation(summary = "添加插件信任密钥")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:plugin:key:add')")
    fun add(@ProcessParam @Valid @RequestBody apiPluginTrustKeyAddParam: ApiPluginTrustKeyAddParam): ResponseResult<ApiPluginTrustKeyVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_INSERT_SUCCESS,
            data = apiPluginTrustKeyService.add(apiPluginTrustKeyAddParam)
        )

    /**
     * Update plugin trust key status
     *
     * @param apiPluginTrustKeyUpdateStatusParam Update plugin trust key status parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKeyUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改插件信任密钥状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:plugin:key:status')")
    fun status(@ProcessParam @Valid @RequestBody apiPluginTrustKeyUpdateStatusParam: ApiPluginTrustKeyUpdateStatusParam): ResponseResult<Unit> {
        apiPluginTrustKeyService.status(apiPluginTrustKeyUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Delete plugin trust key by key ID
     *
     * @param keyId Public key fingerprint
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除插件信任密钥")
    @DeleteMapping("/{keyId}")
    @PreAuthorize("hasAnyAuthority('system:plugin:key:remove')")
    fun delete(@PathVariable keyId: String): ResponseResult<Unit> {
        apiPluginTrustKeyService.deleteByKeyId(keyId)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }
}
