package top.fatweb.apimanagement.controller.user

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.apiKey.*
import top.fatweb.apimanagement.service.system.IApiKeyService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiGroupVo
import top.fatweb.apimanagement.vo.system.ApiKeyVo
import top.fatweb.apimanagement.vo.system.ApiKeyWithSecretVo

/**
 * User-facing API key self-service controller
 *
 * Any authenticated user manages only their own keys.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiKeyService
 */
@BaseController(path = ["/user/api/key"], name = "我的 API Key", description = "用户自助 API Key 管理接口")
class UserApiKeyController(
    private val apiKeyService: IApiKeyService
) {
    /**
     * Get my API key by ID
     *
     * @param id API key ID
     * @return Response object includes API key information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiKeyVo
     */
    @Operation(summary = "获取我的单个 API Key")
    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): ResponseResult<ApiKeyVo> =
        ResponseResult.databaseSuccess(data = apiKeyService.getOne(false, id))

    /**
     * Get my API key paging information
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
    @Operation(summary = "获取我的 API Key")
    @GetMapping
    fun get(@ProcessParam @Valid apiKeyGetParam: ApiKeyGetParam?): ResponseResult<PageVo<ApiKeyVo>> =
        ResponseResult.databaseSuccess(data = apiKeyService.getPage(false, apiKeyGetParam))

    /**
     * Add my API key
     *
     * @param apiKeyAddParam Add API key parameters
     * @return Response object includes API key information and one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyAddParam
     * @see ResponseResult
     * @see ApiKeyWithSecretVo
     */
    @Operation(summary = "创建我的 API Key")
    @PostMapping
    fun add(@ProcessParam @Valid @RequestBody apiKeyAddParam: ApiKeyAddParam): ResponseResult<ApiKeyWithSecretVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.API_PLATFORM_KEY_CREATE_SUCCESS, data = apiKeyService.add(false, apiKeyAddParam)
        )

    /**
     * Update my API key
     *
     * @param apiKeyUpdateParam Update API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改我的 API Key")
    @PutMapping
    fun update(@ProcessParam @Valid @RequestBody apiKeyUpdateParam: ApiKeyUpdateParam): ResponseResult<Unit> {
        apiKeyService.update(false, apiKeyUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update status of my API key
     *
     * @param apiKeyUpdateStatusParam Update status of API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改我的 API Key 状态")
    @PatchMapping
    fun status(@Valid @RequestBody apiKeyUpdateStatusParam: ApiKeyUpdateStatusParam): ResponseResult<Unit> {
        apiKeyService.status(false, apiKeyUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Regenerate secret key of my API key
     *
     * @param id API key ID
     * @return Response object includes new one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiKeyWithSecretVo
     */
    @Operation(summary = "重新生成我的 SecretKey")
    @PostMapping("/{id}/regenerate")
    fun regenerate(@PathVariable id: Long): ResponseResult<ApiKeyWithSecretVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.API_PLATFORM_KEY_REGENERATE_SUCCESS, data = apiKeyService.regenerate(false, id)
        )

    /**
     * Delete my API key by ID
     *
     * @param id API key ID
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除我的 API Key")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseResult<Unit> {
        apiKeyService.deleteOne(false, id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Delete my API keys by list
     *
     * @param apiKeyDeleteParam Delete API key parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyDeleteParam
     * @see ResponseResult
     */
    @Operation(summary = "批量删除我的 API Key")
    @DeleteMapping
    fun deleteList(@Valid @RequestBody apiKeyDeleteParam: ApiKeyDeleteParam): ResponseResult<Unit> {
        apiKeyService.delete(false, apiKeyDeleteParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Get API interfaces I am allowed to grant to a key, grouped by plugin
     *
     * @return Response object includes API interface groups
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see ApiGroupVo
     */
    @Operation(summary = "获取我可授权的 API 列表（按插件分组）")
    @GetMapping("/available-apis")
    fun availableApis(): ResponseResult<List<ApiGroupVo>> =
        ResponseResult.databaseSuccess(data = apiKeyService.availableApis())
}
