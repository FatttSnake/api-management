package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiPluginTrustKey
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyAddParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyUpdateStatusParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiPluginTrustKeyVo

/**
 * Plugin trust key service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiPluginTrustKey
 */
interface IApiPluginTrustKeyService : IService<ApiPluginTrustKey> {
    /**
     * Get a trust key by its fingerprint
     *
     * @param keyId Public key fingerprint
     * @return ApiPluginTrustKey object, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKey
     */
    fun getByKeyId(keyId: String): ApiPluginTrustKey?

    /**
     * Get all trust keys
     *
     * @param apiPluginTrustKeyGetParam Get API plugin trust key parameters
     * @return PageVo<ApiPluginTrustKeyVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKeyGetParam
     * @see ApiPluginTrustKeyVo
     */
    fun get(apiPluginTrustKeyGetParam: ApiPluginTrustKeyGetParam?): PageVo<ApiPluginTrustKeyVo>

    /**
     * Add a trust key; the key ID is derived from the SPKI fingerprint
     *
     * @param apiPluginTrustKeyAddParam Add plugin trust key parameters
     * @return Added ApiPluginTrustKeyVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginTrustKeyVo
     */
    fun add(apiPluginTrustKeyAddParam: ApiPluginTrustKeyAddParam): ApiPluginTrustKeyVo

    /**
     * Update the enable status of a trust key
     *
     * @param apiPluginTrustKeyUpdateStatusParam Update plugin trust key status parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun status(apiPluginTrustKeyUpdateStatusParam: ApiPluginTrustKeyUpdateStatusParam)

    /**
     * Delete a trust key by its fingerprint
     *
     * @param keyId Public key fingerprint
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteByKeyId(keyId: String)
}
