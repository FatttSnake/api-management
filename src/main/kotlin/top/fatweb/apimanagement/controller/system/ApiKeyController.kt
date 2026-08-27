package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiKey.*
import top.fatweb.apimanagement.service.system.IApiKeyService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiKeyVo
import top.fatweb.apimanagement.vo.system.ApiKeyWithSecretVo

/**
 * API key management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiKeyService
 */
@BaseController(path = ["/system/api/key"], name = "API Key 管理", description = "API Key 管理相关接口")
class ApiKeyController(
    private val apiKeyService: IApiKeyService
) {
    /**
     * Get API key by ID
     *
     * @param id API key ID
     * @return Response object includes API key information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiKeyVo
     */
    @Operation(summary = "获取单个 API Key")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:api:key:list')")
    fun getOne(@PathVariable id: Long): ResponseResult<ApiKeyVo> =
        ResponseResult.databaseSuccess(data = apiKeyService.getOne(true, id))

    /**
     * Get API key paging information
     *
     * @param apiKeyGetParam Get API key parameters
     * @return Response object includes API key paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyGetParam
     * @see ResponseResult
     * @see PageVo
     * @see ApiKeyVo
     */
    @Operation(summary = "获取 API Key")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:api:key:list')")
    fun get(@ProcessParam @Valid apiKeyGetParam: ApiKeyGetParam?): ResponseResult<PageVo<ApiKeyVo>> =
        ResponseResult.databaseSuccess(data = apiKeyService.getPage(true, apiKeyGetParam))

    /**
     * Add API key
     *
     * @param apiKeyAddParam Add API key parameters
     * @return Response object includes API key information and one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyAddParam
     * @see ResponseResult
     * @see ApiKeyWithSecretVo
     */
    @Operation(summary = "创建 API Key")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:api:key:add')")
    fun add(@ProcessParam @Valid @RequestBody apiKeyAddParam: ApiKeyAddParam): ResponseResult<ApiKeyWithSecretVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.API_PLATFORM_KEY_CREATE_SUCCESS, data = apiKeyService.add(true, apiKeyAddParam)
        )

    /**
     * Update API key
     *
     * @param apiKeyUpdateParam Update API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API Key")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:api:key:modify')")
    fun update(@ProcessParam @Valid @RequestBody apiKeyUpdateParam: ApiKeyUpdateParam): ResponseResult<Unit> {
        apiKeyService.update(true, apiKeyUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update status of API key
     *
     * @param apiKeyUpdateStatusParam Update status of API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改 API Key 状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:api:key:status')")
    fun status(@Valid @RequestBody apiKeyUpdateStatusParam: ApiKeyUpdateStatusParam): ResponseResult<Unit> {
        apiKeyService.status(true, apiKeyUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Regenerate secret key
     *
     * @param id API key ID
     * @return Response object includes new one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiKeyWithSecretVo
     */
    @Operation(summary = "重新生成 SecretKey")
    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAnyAuthority('system:api:key:secret')")
    fun regenerate(@PathVariable id: Long): ResponseResult<ApiKeyWithSecretVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.API_PLATFORM_KEY_REGENERATE_SUCCESS, data = apiKeyService.regenerate(true, id)
        )

    /**
     * Delete API key by ID
     *
     * @param id API key ID
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除 API Key")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:api:key:delete')")
    fun delete(@PathVariable id: Long): ResponseResult<Unit> {
        apiKeyService.deleteOne(true, id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Delete API key by list
     *
     * @param apiKeyDeleteParam Delete API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyDeleteParam
     * @see ResponseResult
     */
    @Operation(summary = "批量删除 API Key")
    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('system:api:key:delete')")
    fun deleteList(@Valid @RequestBody apiKeyDeleteParam: ApiKeyDeleteParam): ResponseResult<Unit> {
        apiKeyService.delete(true, apiKeyDeleteParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }
}
