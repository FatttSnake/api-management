package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiCallableScope
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.param.system.apiKey.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiKeyAvailableGroupVo
import top.fatweb.apimanagement.vo.api.ApiKeyVo
import top.fatweb.apimanagement.vo.api.ApiKeyWithSecretVo

/**
 * API key service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiKeyService : IService<ApiKey> {
    /**
     * Get API key in page
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param apiKeyGetParam Get API key parameters
     * @return PageVo<ApiKeyVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPage(managed: Boolean, apiKeyGetParam: ApiKeyGetParam?): PageVo<ApiKeyVo>

    /**
     * Get one API key by ID
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param id API key ID
     * @return ApiKeyVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getOne(managed: Boolean, id: Long): ApiKeyVo

    /**
     * Add API key
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param apiKeyAddParam Add API key parameters
     * @return ApiKeyWithSecretVo object includes one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun add(managed: Boolean, apiKeyAddParam: ApiKeyAddParam): ApiKeyWithSecretVo

    /**
     * Update API key
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param apiKeyUpdateParam Update API key parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun update(managed: Boolean, apiKeyUpdateParam: ApiKeyUpdateParam)

    /**
     * Update status of API key
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param apiKeyUpdateStatusParam Update status of API key parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun status(managed: Boolean, apiKeyUpdateStatusParam: ApiKeyUpdateStatusParam)

    /**
     * Delete API key by ID
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param id API key ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteOne(managed: Boolean, id: Long)

    /**
     * Delete API keys by list
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param apiKeyDeleteParam Delete API key parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(managed: Boolean, apiKeyDeleteParam: ApiKeyDeleteParam)

    /**
     * Regenerate secret key
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param id API key ID
     * @return ApiKeyWithSecretVo object includes new one-time secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun regenerate(managed: Boolean, id: Long): ApiKeyWithSecretVo

    /**
     * Get API key by access key
     *
     * @param accessKey Access key
     * @return ApiKey object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getByAccessKey(accessKey: String): ApiKey?

    /**
     * Get API interfaces that may be granted to a key owned by the given owner,
     * grouped by owning plugin
     *
     * A super-admin owner (ID 0) may be granted every enabled interface; any other owner,
     * only the interfaces its account holds as operation codes plus the open DEFAULT ones.
     *
     * @param managed Whether the caller is authorized to manage other users' keys
     * @param userId Owner user ID the grant targets (managed only; null or self-service
     * means the calling user is the owner)
     * @return List of ApiKeyAvailableGroupVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun availableApis(managed: Boolean, userId: Long? = null): List<ApiKeyAvailableGroupVo>

    /**
     * Get the API interfaces the given owner may call
     *
     * A super-admin owner (ID 0) may call every interface; any other owner, only the interfaces
     * its account holds as operation codes plus the open DEFAULT ones. Interfaces of a disabled
     * plugin are never returned.
     *
     * @param ownerId Owner user ID; null means the calling user
     * @param pluginIds Narrow the result to these plugins; null means every plugin
     * @return List of API interfaces callable by the owner, ordered by name
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun callableApis(ownerId: Long? = null, pluginIds: Collection<String>? = null): List<ApiInterface>

    /**
     * Resolve what the given owner may call, as a value a query can be filtered with
     *
     * A super-admin owner (ID 0) is unrestricted; any other owner resolves to the operation
     * codes its account holds, so a caller can filter by the owner's reach inside the database.
     *
     * @param ownerId Owner user ID; null means the calling user
     * @return ApiCallableScope object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun callableScope(ownerId: Long? = null): ApiCallableScope

    /**
     * Record when the key last executed an interface call
     *
     * Best-effort and throttled: at most one database write per key within a fixed window.
     *
     * @param keyId API key ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun touchLastUsedTime(keyId: Long)
}
