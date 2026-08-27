package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.converter.system.toEntity
import top.fatweb.apimanagement.converter.system.toVo
import top.fatweb.apimanagement.converter.system.toVoPage
import top.fatweb.apimanagement.converter.system.toVoWithSecret
import top.fatweb.apimanagement.entity.system.Api
import top.fatweb.apimanagement.entity.system.ApiKey
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.exception.ApiKeyInvalidException
import top.fatweb.apimanagement.exception.ApiKeyPermissionDeniedException
import top.fatweb.apimanagement.mapper.system.ApiKeyMapper
import top.fatweb.apimanagement.param.system.apiKey.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IApiKeyService
import top.fatweb.apimanagement.service.system.IApiService
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiKeyVo
import top.fatweb.apimanagement.vo.system.ApiKeyWithSecretVo
import top.fatweb.apimanagement.vo.system.ApiVo

/**
 * API key service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IApiService
 * @see ServiceImpl
 * @see ApiKeyMapper
 * @see ApiKey
 * @see IApiKeyService
 */
@Service
@DS("master")
class ApiKeyServiceImpl(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiService: IApiService
) : ServiceImpl<ApiKeyMapper, ApiKey>(), IApiKeyService {
    override fun getPage(managed: Boolean, apiKeyGetParam: ApiKeyGetParam?): PageVo<ApiKeyVo> {
        val page = Page<ApiKey>(apiKeyGetParam?.currentPage ?: 1, apiKeyGetParam?.pageSize ?: 20)
        setPageSort(apiKeyGetParam, page, OrderItem.desc("create_time"))

        val loginUserId = getLoginUserIdOrThrow()
        val targetUserId = if (managed) apiKeyGetParam?.userId ?: loginUserId else loginUserId
        val wrapper = KtQueryWrapper(ApiKey()).apply {
            eq(ApiKey::userId, targetUserId)
            apiKeyGetParam?.searchName?.let { like(ApiKey::name, it) }
            apiKeyGetParam?.status?.let { eq(ApiKey::status, if (it) 1 else 0) }
        }

        return this.page(page, wrapper).toVoPage()
    }

    override fun getOne(managed: Boolean, id: Long): ApiKeyVo {
        val key = queryOrThrowException { this.getById(id) }
        ensureOwner(key, managed)
        return key.toVo()
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_CREATE)
    override fun add(managed: Boolean, apiKeyAddParam: ApiKeyAddParam): ApiKeyWithSecretVo {
        val ownerId = if (managed) apiKeyAddParam.userId ?: getLoginUserIdOrThrow() else getLoginUserIdOrThrow()
        val permissions = resolvePermissions(ownerId, apiKeyAddParam.permissionCodes, managed)
        val accessKey = generateAccessKey(SettingsOperator.getValue(ApiSettings::accessKeyLength, 20))
        val secretKey = generateRandomPassword(SettingsOperator.getValue(ApiSettings::secretKeyLength, 40))

        val entity = apiKeyAddParam.toEntity().apply {
            userId = ownerId
            this.accessKey = accessKey
            secretKeyHash = sha256(secretKey)
            this.permissions = permissions.sorted().joinToString(",")
        }
        saveOrThrowException { this.save(entity) }

        return entity.toVoWithSecret(secretKey)
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_UPDATE)
    override fun update(managed: Boolean, apiKeyUpdateParam: ApiKeyUpdateParam) {
        val key = queryOrThrowException { this.getById(apiKeyUpdateParam.id) }
        ensureOwner(key, managed)

        val entity = apiKeyUpdateParam.toEntity()
        apiKeyUpdateParam.permissionCodes?.let {
            entity.permissions = resolvePermissions(key.userId!!, it, managed).sorted().joinToString(",")
        }
        updateOrThrowException { this.updateById(entity) }
        evictCache(key)
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_STATUS)
    override fun status(managed: Boolean, apiKeyUpdateStatusParam: ApiKeyUpdateStatusParam) {
        val key = queryOrThrowException { this.getById(apiKeyUpdateStatusParam.id) }
        ensureOwner(key, managed)

        updateOrThrowException { this.updateById(apiKeyUpdateStatusParam.toEntity()) }
        evictCache(key)
    }

    @Transactional
    override fun deleteOne(managed: Boolean, id: Long) {
        delete(managed, ApiKeyDeleteParam(listOf(id)))
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_DELETE)
    override fun delete(managed: Boolean, apiKeyDeleteParam: ApiKeyDeleteParam) {
        val userId = getLoginUserIdOrThrow()
        apiKeyDeleteParam.ids!!.forEach { id ->
            val key = queryOrThrowException { this.getById(id) }
            if (!managed && key.userId != userId) {
                throw AccessDeniedException("Access denied")
            }
            updateOrThrowException { this.removeById(id) }
            evictCache(key)
        }
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_REGENERATE)
    override fun regenerate(managed: Boolean, id: Long): ApiKeyWithSecretVo {
        val key = queryOrThrowException { this.getById(id) }
        ensureOwner(key, managed)

        val secretKey = generateRandomPassword(SettingsOperator.getValue(ApiSettings::secretKeyLength, 40))
        key.secretKeyHash = sha256(secretKey)
        updateOrThrowException { this.updateById(key) }
        evictCache(key)

        return key.toVoWithSecret(secretKey)
    }

    override fun getByAccessKey(accessKey: String): ApiKey? =
        this.getOne(KtQueryWrapper(ApiKey()).eq(ApiKey::accessKey, accessKey))

    override fun availableApis(): List<ApiVo> {
        val allApis = apiService.list().filter { it.enabled == 1 }
        if (isSuperAdmin(getLoginUserIdOrThrow())) {
            return allApis.map(Api::toVo)
        }
        val ownerCodes = getLoginUser()?.user?.operations?.mapNotNull { it.code }?.toSet() ?: emptySet()
        return allApis.filter { it.code in ownerCodes }.map(Api::toVo)
    }

    private fun isSuperAdmin(userId: Long): Boolean = userId == 0L

    private fun ensureOwner(key: ApiKey, managed: Boolean) {
        if (!managed && key.userId != getLoginUserIdOrThrow()) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun resolvePermissions(ownerId: Long, requestCodes: List<String>?, grantAny: Boolean): Set<String> {
        val allCodes = apiService.list().filter { it.enabled == 1 }.mapNotNull { it.code }.toSet()
        val ownerCodes = if (grantAny || isSuperAdmin(ownerId)) {
            allCodes
        } else {
            getLoginUser()?.user?.operations?.mapNotNull { it.code }?.toSet() ?: emptySet()
        }
        val requestSet = requestCodes?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

        if (requestSet.isEmpty()) {
            return ownerCodes.intersect(allCodes)
        }

        requestSet.forEach {
            if (it !in ownerCodes || it !in allCodes) {
                throw ApiKeyPermissionDeniedException()
            }
        }
        return requestSet
    }

    private fun generateAccessKey(length: Int): String {
        repeat(3) {
            val candidate = generateRandomPassword(length)
            if (getByAccessKey(candidate) == null) {
                return candidate
            }
        }
        throw ApiKeyInvalidException()
    }

    private fun evictCache(key: ApiKey) {
        key.accessKey?.let { redisProvider.delObject(cacheKey(it)) }
    }

    private fun cacheKey(accessKey: String) = "${serverProperties.security.tokenIssuer}_apikey_$accessKey"
}
