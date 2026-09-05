package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.converter.api.toEntity
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.converter.api.toVoWithSecret
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.exception.ApiKeyInvalidException
import top.fatweb.apimanagement.exception.ApiKeyPermissionDeniedException
import top.fatweb.apimanagement.mapper.api.ApiKeyMapper
import top.fatweb.apimanagement.mapper.permission.UserMapper
import top.fatweb.apimanagement.param.system.apiKey.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiKeyVo
import top.fatweb.apimanagement.vo.api.ApiKeyWithSecretVo
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * API key service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see UserMapper
 * @see IApiPluginService
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
    private val userMapper: UserMapper,
    private val apiPluginService: IApiPluginService
) : ServiceImpl<ApiKeyMapper, ApiKey>(), IApiKeyService {
    private companion object {
        /**
         * Window in seconds between two consecutive last-used-time writes of the same
         * key: keeps a hot key down to ~1 UPDATE per window instead of one per request.
         */
        const val TOUCH_WINDOW_SECONDS = 60L
    }

    override fun getPage(managed: Boolean, apiKeyGetParam: ApiKeyGetParam?): PageVo<ApiKeyVo> {
        val page = Page<ApiKey>(apiKeyGetParam?.currentPage ?: 1, apiKeyGetParam?.pageSize ?: 20)
        setPageSort(apiKeyGetParam, page, OrderItem.desc("create_time"))

        val loginUserId = getLoginUserIdOrThrow()
        val targetUserId = if (managed) apiKeyGetParam?.userId ?: loginUserId else loginUserId
        val wrapper = KtQueryWrapper(ApiKey()).apply {
            eq(ApiKey::userId, targetUserId)
            apiKeyGetParam?.searchName?.let { like(ApiKey::name, it) }
            apiKeyGetParam?.enable?.let { eq(ApiKey::enable, if (it) 1 else 0) }
        }

        return page(page, wrapper).toVoPage()
    }

    override fun getOne(managed: Boolean, id: Long): ApiKeyVo {
        val key = queryOrThrowException { getById(id) }
        ensureOwner(key, managed)
        return key.toVo()
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_CREATE)
    override fun add(managed: Boolean, apiKeyAddParam: ApiKeyAddParam): ApiKeyWithSecretVo {
        val ownerId = if (managed) apiKeyAddParam.userId ?: getLoginUserIdOrThrow() else getLoginUserIdOrThrow()
        val permissions = resolvePermissions(ownerId, apiKeyAddParam.permissionCodes)
        val accessKey = generateAccessKey(SettingsOperator.getValue(ApiSettings::accessKeyLength, 20))
        val secretKey = generateRandomPassword(SettingsOperator.getValue(ApiSettings::secretKeyLength, 40))

        val entity = apiKeyAddParam.toEntity().apply {
            this.userId = ownerId
            this.accessKey = accessKey
            this.secretKeyHash = sha256(secretKey)
            this.permissions = permissions.sorted().joinToString(",")
        }
        saveOrThrowException { save(entity) }

        return entity.toVoWithSecret(secretKey)
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_UPDATE)
    override fun update(managed: Boolean, apiKeyUpdateParam: ApiKeyUpdateParam) {
        val key = queryOrThrowException { getById(apiKeyUpdateParam.id) }
        ensureOwner(key, managed)

        updateOrThrowException {
            update(
                KtUpdateWrapper(ApiKey()).apply {
                    eq(ApiKey::id, apiKeyUpdateParam.id)
                    set(ApiKey::name, apiKeyUpdateParam.name)
                    set(
                        ApiKey::permissions,
                        apiKeyUpdateParam.permissionCodes?.let {
                            resolvePermissions(key.userId!!, it).sorted().joinToString(",")
                        }
                    )
                    set(ApiKey::enable, apiKeyUpdateParam.enable)
                    set(ApiKey::expireTime, apiKeyUpdateParam.expireTime)
                    set(ApiKey::ipWhitelist, apiKeyUpdateParam.ipWhitelist)
                    set(ApiKey::rateLimit, apiKeyUpdateParam.rateLimit)
                    set(ApiKey::quota, apiKeyUpdateParam.quota)
                    set(ApiKey::quotaPeriod, apiKeyUpdateParam.quotaPeriod)
                    set(ApiKey::remark, apiKeyUpdateParam.remark)
                }
            )
        }
        evictCache(key)
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_STATUS)
    override fun status(managed: Boolean, apiKeyUpdateStatusParam: ApiKeyUpdateStatusParam) {
        val key = queryOrThrowException { getById(apiKeyUpdateStatusParam.id) }
        ensureOwner(key, managed)

        updateOrThrowException { updateById(apiKeyUpdateStatusParam.toEntity()) }
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
            val key = queryOrThrowException { getById(id) }
            if (!managed && key.userId != userId) {
                throw AccessDeniedException("Access denied")
            }
            updateOrThrowException { removeById(id) }
            evictCache(key)
        }
    }

    @Transactional
    @EventLogRecord(EventLog.Event.KEY_REGENERATE)
    override fun regenerate(managed: Boolean, id: Long): ApiKeyWithSecretVo {
        val key = queryOrThrowException { getById(id) }
        ensureOwner(key, managed)

        val secretKey = generateRandomPassword(SettingsOperator.getValue(ApiSettings::secretKeyLength, 40))
        key.secretKeyHash = sha256(secretKey)
        updateOrThrowException { updateById(key) }
        evictCache(key)

        return key.toVoWithSecret(secretKey)
    }

    override fun getByAccessKey(accessKey: String): ApiKey? =
        getOne(KtQueryWrapper(ApiKey()).eq(ApiKey::accessKey, accessKey))

    override fun touchLastUsedTime(keyId: Long) {
        val gateKey = "${serverProperties.security.tokenIssuer}_apikey_lastused:$keyId"
        // Distributed gate: only the first caller in each window owns the write, so a hot
        // key is refreshed at most once per TOUCH_WINDOW_SECONDS instead of on every request.
        if (!redisProvider.setIfAbsent(gateKey, TOUCH_WINDOW_SECONDS)) {
            return
        }
        // Wrapper-based partial update: bypasses the optimistic-lock version check and does
        // not auto-fill update_time — a "last used" touch should not count as an edit.
        update(KtUpdateWrapper(ApiKey()).apply {
            eq(ApiKey::id, keyId)
            set(ApiKey::lastUsedTime, LocalDateTime.now(ZoneOffset.UTC))
        })
    }

    override fun availableApis(managed: Boolean, userId: Long?): List<ApiGroupVo> {
        val ownerId = if (managed) userId ?: getLoginUserIdOrThrow() else getLoginUserIdOrThrow()
        val allApis = apiPluginService.listEnabledInterfaces()
        return selectableApis(ownerId, allApis)
            .groupBy { it.pluginId ?: "" }
            .map { (pluginId, apis) ->
                val plugin = apiPluginService.getByPluginId(pluginId)

                ApiGroupVo(
                    pluginId = pluginId.ifBlank { null },
                    pluginName = plugin?.name,
                    pluginDescription = plugin?.description,
                    interfaces = apis.map(ApiInterface::toVo)
                )
            }
    }

    private fun isSuperAdmin(userId: Long): Boolean = userId == 0L

    private fun ensureOwner(key: ApiKey, managed: Boolean) {
        if (!managed && key.userId != getLoginUserIdOrThrow()) {
            throw AccessDeniedException("Access denied")
        }
    }

    /**
     * Get operation codes the owner holds. For the calling user itself the login-time
     * snapshot is reused so the scope matches what the account-path interceptor enforces
     * for the current token; any other owner is resolved fresh from the database.
     *
     * @param ownerId Owner user ID
     * @return Set of operation codes the owner holds, empty when the owner is missing
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun operationCodesOf(ownerId: Long): Set<String> =
        if (ownerId == getLoginUserId()) {
            getLoginUser()?.user?.operations?.mapNotNullTo(LinkedHashSet()) { it.code } ?: emptySet()
        } else {
            userMapper.selectOneWithPowerInfoById(ownerId)?.operations?.mapNotNullTo(LinkedHashSet()) { it.code }
                ?: emptySet()
        }

    /**
     * Resolve the API interfaces that may be granted to a key owned by the given owner:
     * a super-admin owner (ID 0) may grant every enabled interface, any other owner only
     * the interfaces its account is authorized for (operation codes) plus the open DEFAULT
     * ones. Single source of truth shared by [availableApis] and [resolvePermissions].
     *
     * @param ownerId Owner user ID
     * @param allApis All enabled API interfaces
     * @return List of API interfaces grantable for the owner
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     */
    private fun selectableApis(ownerId: Long, allApis: List<ApiInterface>): List<ApiInterface> {
        if (isSuperAdmin(ownerId)) {
            return allApis
        }

        val ownerCodes = operationCodesOf(ownerId)
        return allApis.filter {
            it.code in ownerCodes || apiPluginService.resolveAccessMode(it) == ApiInterface.AccessMode.DEFAULT
        }
    }

    private fun resolvePermissions(ownerId: Long, requestCodes: List<String>?): Set<String> {
        val requestSet = requestCodes?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

        // Only explicitly checked APIs enter the key's permissions, for both managed and
        // self-service keys — an unchecked key cannot access any /api/** interface.
        if (requestSet.isEmpty()) {
            return emptySet()
        }

        val selectable = selectableApis(ownerId, apiPluginService.listEnabledInterfaces())
            .mapNotNull { it.code }.toSet()
        requestSet.forEach {
            if (it !in selectable) {
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
