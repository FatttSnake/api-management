package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiPluginTrustKey
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.mapper.api.ApiPluginTrustKeyMapper
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyAddParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginTrustKeyUpdateStatusParam
import top.fatweb.apimanagement.sdk.plugin.PluginSigner
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import top.fatweb.apimanagement.util.getLoginUserIdOrThrow
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiPluginTrustKeyVo

/**
 * Plugin trust key service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ApiPluginTrustKeyMapper
 * @see ApiPluginTrustKey
 * @see IApiPluginTrustKeyService
 */
@Service
@DS("master")
class ApiPluginTrustKeyServiceImpl : ServiceImpl<ApiPluginTrustKeyMapper, ApiPluginTrustKey>(),
    IApiPluginTrustKeyService {
    override fun getByKeyId(keyId: String): ApiPluginTrustKey? =
        getOne(KtQueryWrapper(ApiPluginTrustKey()).eq(ApiPluginTrustKey::keyId, keyId))

    override fun get(apiPluginTrustKeyGetParam: ApiPluginTrustKeyGetParam?): PageVo<ApiPluginTrustKeyVo> {
        val page = Page<ApiPluginTrustKey>(
            apiPluginTrustKeyGetParam?.currentPage ?: 1,
            apiPluginTrustKeyGetParam?.pageSize ?: 20
        )
        setPageSort(apiPluginTrustKeyGetParam, page, OrderItem.asc("create_time"))

        val wrapper = KtQueryWrapper(ApiPluginTrustKey()).apply {
            apiPluginTrustKeyGetParam?.searchAlias?.let { like(ApiPluginTrustKey::alias, it) }
            apiPluginTrustKeyGetParam?.enable?.let { eq(ApiPluginTrustKey::enable, if (it) 1 else 0) }
        }

        return page(page, wrapper).toVoPage()
    }

    @Transactional
    override fun add(apiPluginTrustKeyAddParam: ApiPluginTrustKeyAddParam): ApiPluginTrustKeyVo {
        val keyId = try {
            PluginSigner.spkiFingerprint(apiPluginTrustKeyAddParam.publicKey)
        } catch (e: Exception) {
            throw PluginInstallException("Invalid public key PEM: ${e.message}")
        }
        if (getByKeyId(keyId) != null) {
            throw PluginInstallException("Trust key already exists: $keyId")
        }

        val entity = ApiPluginTrustKey().apply {
            this.keyId = keyId
            this.alias = apiPluginTrustKeyAddParam.alias
            this.publicKey = apiPluginTrustKeyAddParam.publicKey
            this.enable = 1
            this.createdBy = getLoginUserIdOrThrow()
        }
        saveOrThrowException { save(entity) }

        return entity.toVo()
    }

    @Transactional
    override fun status(apiPluginTrustKeyUpdateStatusParam: ApiPluginTrustKeyUpdateStatusParam) {
        val entity = getByKeyId(apiPluginTrustKeyUpdateStatusParam.keyId)
            ?: throw PluginInstallException("Trust key not found: ${apiPluginTrustKeyUpdateStatusParam.keyId}")
        entity.enable = if (apiPluginTrustKeyUpdateStatusParam.enable) 1 else 0
        updateOrThrowException { updateById(entity) }
    }

    @Transactional
    override fun deleteByKeyId(keyId: String) {
        val entity = getByKeyId(keyId) ?: throw PluginInstallException("Trust key not found: $keyId")
        removeById(entity.id)
    }
}
