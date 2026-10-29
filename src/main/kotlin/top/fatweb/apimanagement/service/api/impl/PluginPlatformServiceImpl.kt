package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import top.fatweb.apimanagement.component.plugin.PluginArtifactStore
import top.fatweb.apimanagement.component.plugin.PluginConfigFieldType
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaCache
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
import top.fatweb.apimanagement.component.plugin.PluginConfigValueAction
import top.fatweb.apimanagement.component.plugin.PluginDatasourceFactory
import top.fatweb.apimanagement.component.plugin.PluginEndpointRegistrar
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.component.plugin.PluginSettingReader
import top.fatweb.apimanagement.component.plugin.runtime.PluginOpenApiBuilder
import top.fatweb.apimanagement.component.plugin.runtime.PluginRuntimeRegistry
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.converter.api.toGroupVo
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.exception.PluginNotTrustedException
import top.fatweb.apimanagement.exception.PluginPurgeFailedException
import top.fatweb.apimanagement.exception.PluginSignatureInvalidException
import top.fatweb.apimanagement.exception.PluginVersionConflictException
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateStatusParam
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.runtime.LoadedPlugin
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginDescriptor
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiInterfaceService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.service.api.IPluginModuleService
import top.fatweb.apimanagement.service.api.IPluginPlatformService
import top.fatweb.apimanagement.util.PluginCryptoUtil
import top.fatweb.apimanagement.util.PluginDatasourceUtil
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo
import java.nio.file.Files
import java.time.LocalDateTime

/**
 * Module-based plugin platform.
 *
 * The runtime is rebuilt entirely from a plugin jar and its lock; the gateway never resolves
 * dependency versions at runtime.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
@DS("master")
@Order(Ordered.LOWEST_PRECEDENCE)
class PluginPlatformServiceImpl(
    private val serverProperties: ServerProperties,
    private val artifactStore: PluginArtifactStore,
    private val endpointRegistrar: PluginEndpointRegistrar,
    private val runtimeRegistry: PluginRuntimeRegistry,
    private val moduleService: IPluginModuleService,
    private val openApiBuilder: PluginOpenApiBuilder,
    private val configSchemaCache: PluginConfigSchemaCache,
    private val registryCache: PluginRegistryCache,
    private val pluginSettingReader: PluginSettingReader,
    private val datasourceFactory: PluginDatasourceFactory,
    private val settingService: IApiPluginSettingService,
    private val fileStorageProvider: FileStorageProvider,
    private val apiInterfaceService: IApiInterfaceService,
) : ServiceImpl<ApiPluginMapper, ApiPlugin>(),
    IPluginPlatformService,
    ApplicationRunner {
    private val logger = LoggerFactory.getLogger(PluginPlatformServiceImpl::class.java)

    /**
     * Loads installed plugins at startup.
     *
     * @param args application arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun run(args: ApplicationArguments) {
        loadInstalled()
    }

    /**
     * Installs or upgrades a plugin.
     *
     * @param jarBytes plugin jar
     * @param jarName original jar name
     * @return installed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun install(jarBytes: ByteArray, jarName: String): ApiPlugin {
        if (jarBytes.size > serverProperties.storage.pluginJarMaxBytes) {
            throw PluginInstallException(
                "Plugin jar is ${jarBytes.size} bytes, limit is ${serverProperties.storage.pluginJarMaxBytes}",
            )
        }

        val jar = artifactStore.store(jarBytes)
        var pluginId: String? = null
        try {
            val signerKeyId = artifactStore.verify(jar.path)
            val descriptor = artifactStore.descriptor(jar.path)
            pluginId = descriptor.pluginId
            return runtimeRegistry.withLock(pluginId) {
                installLocked(descriptor, jar, jarName, signerKeyId)
            }
        } catch (ex: Throwable) {
            // The jar is content-addressed: a byte-identical reinstall is the jar the installed
            // row already points at, and must survive a failed attempt
            if (jar.created && pluginId?.let { get(it)?.fileHash == jar.digest } != true) {
                runCatching { Files.deleteIfExists(jar.path) }
            }
            throw asInstallFailure(ex)
        }
    }

    /**
     * Applies an install after the plugin ID is known and its lock is held.
     *
     * Every check that does not need the new code running happens before the current runtime is
     * taken down, and a failure after that point restores it once the transaction rolls back.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun installLocked(
        descriptor: PluginDescriptor,
        jar: PluginArtifactStore.StoredJar,
        jarName: String,
        signerKeyId: String,
    ): ApiPlugin {
        val pluginId = descriptor.pluginId
        val previousRow = get(pluginId)
        if (previousRow != null && (previousRow.versionCode ?: 0) > descriptor.versionCode) {
            throw PluginVersionConflictException(
                "Plugin $pluginId version ${descriptor.versionCode} is older than " +
                    "installed version ${previousRow.versionCode}",
            )
        }

        val dependency = artifactStore.lock(jar.path)
        val lock = dependency.lock
        if (descriptor.dependencies.isNotEmpty() && lock.modules.isEmpty()) {
            throw PluginInstallException(
                "Plugin $pluginId declares dependencies but has no dependency lock",
            )
        }
        val schemaJson = artifactStore.configSchema(jar.path)
        val schema = PluginConfigSchemaUtil.parseOrThrow(schemaJson)

        val previousSchema = configSchemaCache.get(pluginId)
        val previousHash = previousRow?.fileHash
        val wasRunning = runtimeRegistry.isRunning(pluginId)
        val enabled = previousRow?.enable != 0
        var loaded: LoadedPlugin? = null
        try {
            moduleService.installForPlugin(pluginId, lock)

            // The old runtime is taken down only after the artifact, descriptor, version,
            // dependency lock and config schema have all been accepted
            if (wasRunning) {
                runtimeRegistry.unload(pluginId, purge = false, uninstalling = true)
            }
            if (enabled) {
                loaded = runtimeRegistry.load(pluginId, jar.path, schema)
            }

            val row =
                upsertPluginRow(
                    descriptor = descriptor,
                    digest = jar.digest,
                    jarName = jarName,
                    signerKeyId = signerKeyId,
                    schemaJson = schemaJson,
                    openapiJson = loaded?.let { openApiBuilder.build(pluginId, it.routes) },
                    lockJson = dependency.bytes?.toString(Charsets.UTF_8),
                    lockHash = dependency.hash(artifactStore::sha256),
                    lock = lock,
                )

            if (loaded != null) {
                endpointRegistrar.register(row, loaded.routes)
                runtimeRegistry.activate(pluginId, loaded)
                updateLoadState(row, "LOADED", null, null)
            } else {
                updateLoadState(row, "DISABLED", null, null)
            }

            if (previousHash != null && previousHash != jar.digest) {
                runCatching { Files.deleteIfExists(artifactStore.path(previousHash)) }.onFailure {
                    logger.warn("Cannot delete the previous jar of plugin {}", pluginId, it)
                }
            }
            onCommitted { refreshCache() }
            return row
        } catch (ex: Throwable) {
            configSchemaCache.put(pluginId, previousSchema)
            loaded?.let { runtimeRegistry.discard(pluginId, it) }
            if (wasRunning) {
                scheduleRuntimeRestore(previousRow)
            }
            throw ex
        }
    }

    /**
     * Uninstalls a plugin.
     *
     * @param pluginId plugin ID
     * @param purgeData whether to delete plugin-owned data
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun uninstall(pluginId: String, purgeData: Boolean) {
        runtimeRegistry.withLock(pluginId) {
            val row = get(pluginId) ?: return@withLock
            val wasRunning = runtimeRegistry.isRunning(pluginId)

            // onPurge lives in the plugin, so a disabled plugin has to be mounted to run it.
            // Failing to mount is a purge failure, not a reason to delete the data anyway.
            if (purgeData && !wasRunning) {
                val mounted =
                    try {
                        runtimeRegistry.load(row)
                    } catch (ex: Throwable) {
                        throw PluginPurgeFailedException(
                            "Cannot mount plugin $pluginId to run onPurge: ${ex.message}",
                            ex,
                        )
                    }
                runtimeRegistry.activate(pluginId, mounted)
            }

            try {
                runtimeRegistry.unload(pluginId, purge = purgeData, uninstalling = true)
            } catch (ex: PluginPurgeFailedException) {
                if (wasRunning) {
                    scheduleRuntimeRestore(row)
                }
                throw ex
            }
            moduleService.release(pluginId)

            endpointRegistrar.remove(pluginId)
            row.id?.let { removeById(it) }

            if (purgeData) {
                settingService.deleteByPlugin(pluginId)
                datasourceFactory.removeData(pluginId)
                runCatching {
                    fileStorageProvider.deleteAtPrefix(StorageKeyUtil.pluginBaseKey(pluginId))
                }.onFailure {
                    logger.warn("Cannot delete stored files of plugin {}", pluginId, it)
                }
            }
            row.fileHash?.let { hash -> runCatching { Files.deleteIfExists(artifactStore.path(hash)) } }
            onCommitted {
                configSchemaCache.put(pluginId, null)
                refreshCache()
            }
        }
    }

    /**
     * Enables or disables a plugin.
     *
     * @param pluginId plugin ID
     * @param enabled desired state
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun setEnabled(pluginId: String, enabled: Boolean) {
        runtimeRegistry.withLock(pluginId) {
            val row = get(pluginId) ?: throw PluginInstallException("Plugin not found: $pluginId")
            val wasEnabled = row.enable == 1
            if (enabled == wasEnabled) {
                return@withLock
            }

            if (enabled) {
                val runtime = runtimeRegistry.load(row)
                endpointRegistrar.register(row, runtime.routes)
                runtimeRegistry.activate(pluginId, runtime)
                row.enable = 1
            } else {
                // A disabled plugin does not run at all: its hooks, timers and event subscriptions
                // are taken down with the routes. Module references stay, so enabling it again
                // reloads the same modules without a re-upload.
                runtimeRegistry.unload(pluginId, purge = false, uninstalling = false)
                row.enable = 0
            }
            updateOrThrowException { updateById(row) }
            updateLoadState(row, if (enabled) "LOADED" else "DISABLED", null, null)
            onCommitted { refreshCache() }
        }
    }

    /**
     * Reloads a plugin from its stored jar.
     *
     * @param pluginId plugin ID
     * @return refreshed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional(noRollbackFor = [PluginInstallException::class])
    override fun reload(pluginId: String): ApiPlugin = runtimeRegistry.withLock(pluginId) {
        val row = get(pluginId) ?: throw PluginInstallException("Plugin not found: $pluginId")
        if (row.enable != 1) {
            throw PluginInstallException("Plugin $pluginId is disabled")
        }
        runtimeRegistry.unload(pluginId, purge = false, uninstalling = false)
        val loaded =
            try {
                runtimeRegistry.load(row)
            } catch (ex: Throwable) {
                // Committed on purpose: a plugin that cannot start has to be reported as such,
                // rather than left looking loaded with no runtime behind it
                updateLoadState(row, "LOAD_ERROR", "STARTUP_ERROR", ex.message?.take(MAX_ERROR_LENGTH))
                onCommitted { refreshCache() }
                throw PluginInstallException("Cannot reload plugin $pluginId: ${ex.message}", ex)
            }
        endpointRegistrar.register(row, loaded.routes)
        runtimeRegistry.activate(pluginId, loaded)
        row.openapi = openApiBuilder.build(pluginId, loaded.routes)
        updateOrThrowException { updateById(row) }
        updateLoadState(row, "LOADED", null, null)
        onCommitted { refreshCache() }
        row
    }

    /**
     * Finds a plugin.
     *
     * @param pluginId plugin ID
     * @return plugin or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun get(pluginId: String): ApiPlugin? =
        getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))

    override fun getPluginPage(param: ApiPluginGetParam?): PageVo<ApiPluginVo> {
        val page = Page<ApiPlugin>(param?.currentPage ?: 1, param?.pageSize ?: 20)
        setPageSort(param, page, OrderItem.desc("create_time"))
        val wrapper =
            KtQueryWrapper(ApiPlugin()).apply {
                param?.searchName?.let { like(ApiPlugin::name, it) }
                param?.enable?.let { eq(ApiPlugin::enable, if (it) 1 else 0) }
            }
        return page(page, wrapper).toVoPage()
    }

    override fun getPlugin(pluginId: String): ApiPluginVo = get(pluginId)?.toVo() ?: throw NoRecordFoundException()

    @Transactional
    override fun updatePlugin(param: ApiPluginUpdateParam) {
        val existing = param.id?.let(::getById) ?: throw NoRecordFoundException()
        val pluginId = existing.pluginId ?: throw NoRecordFoundException()

        runtimeRegistry.withLock(pluginId) {
            val wasEnabled = existing.enable == 1
            update(
                KtUpdateWrapper(ApiPlugin()).apply {
                    eq(ApiPlugin::id, param.id)
                    param.enable?.let { set(ApiPlugin::enable, if (it) 1 else 0) }
                    set(ApiPlugin::defaultPrice, param.defaultPrice)
                    set(ApiPlugin::defaultRateLimit, param.defaultRateLimit)
                    set(ApiPlugin::defaultAccessMode, param.defaultAccessMode)
                    set(ApiPlugin::updateTime, LocalDateTime.now())
                },
            )

            // Price, rate limit and access mode also feed the registry cache, so the refresh
            // must not be conditional on `enable` being present
            val nowEnabled = param.enable
            if (nowEnabled != null && nowEnabled != wasEnabled) {
                setEnabled(pluginId, nowEnabled)
            }
            onCommitted { refreshCache() }
        }
    }

    override fun getInterfacePage(param: ApiInterfaceGetParam?): PageVo<ApiGroupVo> {
        val current = param?.currentPage ?: 1
        val size = param?.pageSize ?: 20

        fun KtQueryWrapper<ApiInterface>.addInterfaceFilter() {
            param?.searchCode?.let { like(ApiInterface::code, it) }
            param?.searchName?.let { like(ApiInterface::name, it) }
            param?.pluginId?.let { eq(ApiInterface::pluginId, it) }
            param?.enable?.let { eq(ApiInterface::enable, if (it) 1 else 0) }
        }

        val pluginIds =
            apiInterfaceService
                .listObjs<Any>(
                    QueryWrapper<ApiInterface>().select("DISTINCT plugin_id").apply {
                        param?.searchCode?.let { like("code", it) }
                        param?.searchName?.let { like("name", it) }
                        param?.pluginId?.let { eq("plugin_id", it) }
                        param?.enable?.let { eq("enable", if (it) 1 else 0) }
                    },
                ).mapNotNull { it?.toString() }
                .filter { it.isNotBlank() }
        if (pluginIds.isEmpty()) {
            return PageVo(total = 0, pages = 0, size = size, current = current, records = emptyList())
        }

        val pluginById =
            list(
                KtQueryWrapper(ApiPlugin()).`in`(ApiPlugin::pluginId, pluginIds),
            ).associateBy { it.pluginId }
        val orderedPluginIds =
            pluginIds.sortedWith(
                compareByDescending<String> { id -> pluginById[id]?.createTime }.thenByDescending { it },
            )
        val total = orderedPluginIds.size.toLong()
        val pages = (total + size - 1) / size
        val from = ((current - 1) * size).coerceAtLeast(0).toInt()
        val pagePluginIds = orderedPluginIds.drop(from).take(size.toInt())
        if (pagePluginIds.isEmpty()) {
            return PageVo(total = total, pages = pages, size = size, current = current, records = emptyList())
        }

        val interfaces =
            apiInterfaceService
                .list(
                    KtQueryWrapper(ApiInterface())
                        .apply { addInterfaceFilter() }
                        .`in`(ApiInterface::pluginId, pagePluginIds),
                ).sortedWith(compareByDescending<ApiInterface> { it.createTime }.thenByDescending { it.id })

        val interfacesByPlugin = interfaces.groupBy { it.pluginId ?: "" }
        val records =
            pagePluginIds.map { pluginId ->
                ApiGroupVo(
                    pluginId = pluginId,
                    pluginName = pluginById[pluginId]?.name,
                    pluginDescription = pluginById[pluginId]?.description,
                    interfaces = interfacesByPlugin[pluginId].orEmpty().map(ApiInterface::toVo),
                )
            }
        return PageVo(total = total, pages = pages, size = size, current = current, records = records)
    }

    override fun updateInterface(param: ApiInterfaceUpdateParam) {
        updateOrThrowException(NoRecordFoundException()) {
            apiInterfaceService.update(
                KtUpdateWrapper(ApiInterface()).apply {
                    eq(ApiInterface::id, param.id)
                    set(ApiInterface::price, param.price)
                    set(ApiInterface::billingMode, param.billingMode)
                    param.needKey?.let { set(ApiInterface::needKey, if (it) 1 else 0) }
                    set(ApiInterface::rateLimit, param.rateLimit)
                    param.enable?.let { set(ApiInterface::enable, if (it) 1 else 0) }
                    set(ApiInterface::accessMode, param.accessMode)
                    set(ApiInterface::updateTime, LocalDateTime.now())
                },
            )
        }
        onCommitted { refreshCache() }
    }

    override fun updateInterfaceStatus(param: ApiInterfaceUpdateStatusParam) {
        updateOrThrowException(NoRecordFoundException()) {
            apiInterfaceService.update(
                KtUpdateWrapper(ApiInterface()).apply {
                    eq(ApiInterface::id, param.id)
                    set(ApiInterface::enable, if (param.enable == true) 1 else 0)
                    set(ApiInterface::updateTime, LocalDateTime.now())
                },
            )
        }
        refreshCache()
    }

    override fun getPluginConfig(pluginId: String): ApiPluginConfigVo {
        val schema = configSchemaOf(pluginId)
        val stored = settingService.listByPlugin(pluginId)
        val unreadable =
            schema
                ?.secretKeys()
                .orEmpty()
                .filterNot { pluginSettingReader.isReadable(pluginId, it) }
                .toSet()

        return ApiPluginConfigVo(
            pluginId = pluginId,
            groups = schema?.toGroupVo(stored, unreadable) ?: emptyList(),
            datasources =
            schema
                ?.datasources
                ?.map { declaration ->
                    declaration.toVo(
                        configured =
                        PluginDatasourceUtil.isConfigured(declaration) { key ->
                            stored[key] ?: schema.fieldOf(key)?.default
                        },
                    )
                }.orEmpty(),
        )
    }

    @Transactional
    override fun updatePluginConfig(param: ApiPluginConfigUpdateParam) {
        val pluginId = param.pluginId!!
        val schema =
            configSchemaOf(pluginId)
                ?: throw IllegalArgumentException("Plugin '$pluginId' does not declare any configuration")
        val submitted = submittedGroupsOf(param)
        val stored = pluginSettingReader.resolveAll(pluginId)
        PluginConfigSchemaUtil.validate(schema, submitted, stored)
        val effective = PluginConfigSchemaUtil.effectiveValues(schema, submitted, stored)
        schema.datasources.forEach { declaration ->
            PluginDatasourceUtil.resolve(declaration) { key -> effective[key] }
        }
        val previous = PluginConfigSchemaUtil.effectiveValues(schema, emptyMap(), stored)
        val datasourceChanged = schema.datasourceKeys.any { previous[it] != effective[it] }
        val down = get(pluginId)?.loadError != null

        submitted.values.forEach { values ->
            values.forEach { (key, value) ->
                val field = schema.fieldOf(key) ?: return@forEach
                when (PluginConfigSchemaUtil.actionOf(field, value)) {
                    PluginConfigValueAction.KEEP -> Unit
                    PluginConfigValueAction.CLEAR -> settingService.delete(pluginId, key)
                    PluginConfigValueAction.SET -> {
                        val set = value!!
                        settingService.set(
                            pluginId,
                            key,
                            if (field.type == PluginConfigFieldType.SECRET) {
                                PluginCryptoUtil.encrypt(serverProperties.security.tokenSecret, set)
                            } else {
                                set
                            },
                        )
                    }
                }
            }
        }

        if (datasourceChanged || down) {
            onCommitted {
                runCatching { reload(pluginId) }
                    .onFailure { logger.error("Cannot remount plugin {} after a config change", pluginId, it) }
            }
        }
    }

    override fun testPluginDatasource(pluginId: String, name: String, values: Map<String, String?>) {
        if (get(pluginId) == null) {
            throw NoRecordFoundException()
        }
        datasourceFactory.test(pluginId, name, values)
    }

    private fun configSchemaOf(pluginId: String): PluginConfigSchema? {
        if (get(pluginId) == null) {
            throw NoRecordFoundException()
        }
        return configSchemaCache.get(pluginId)
    }

    private fun submittedGroupsOf(param: ApiPluginConfigUpdateParam): Map<String, Map<String, String?>> {
        val groups = param.groups.orEmpty()
        requireDistinct(groups.mapNotNull { it.key }, "Plugin config groups are submitted")
        return groups.associate { group ->
            val groupKey = group.key!!
            val values = group.values.orEmpty()
            requireDistinct(values.mapNotNull { it.key }, "Plugin config group '$groupKey' submits a key")
            groupKey to values.associate { it.key!! to it.value }
        }
    }

    private fun requireDistinct(names: List<String>, what: String) {
        val duplicated =
            names
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicated.isEmpty()) { "$what more than once: ${duplicated.joinToString()}" }
    }

    private fun onCommitted(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = action()
                },
            )
        } else {
            action()
        }
    }

    /**
     * Loads every enabled plugin from disk.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun loadInstalled() {
        list().forEach { row ->
            val pluginId = row.pluginId ?: return@forEach
            runtimeRegistry.withLock(pluginId) {
                if (row.enable != 1) {
                    updateLoadState(row, "DISABLED", null, null)
                    return@withLock
                }
                if (runtimeRegistry.isRunning(pluginId)) {
                    return@withLock
                }
                try {
                    val loaded = runtimeRegistry.load(row)
                    runtimeRegistry.activate(pluginId, loaded)
                    row.openapi = openApiBuilder.build(pluginId, loaded.routes)
                    updateOrThrowException { updateById(row) }
                    updateLoadState(row, "LOADED", null, null)
                } catch (ex: Throwable) {
                    logger.error("Cannot load plugin {} at startup", pluginId, ex)
                    updateLoadState(row, "LOAD_ERROR", "STARTUP_ERROR", ex.message?.take(MAX_ERROR_LENGTH))
                }
            }
        }
        refreshCache()
    }

    private fun upsertPluginRow(
        descriptor: PluginDescriptor,
        digest: String,
        jarName: String,
        signerKeyId: String,
        schemaJson: String?,
        openapiJson: String?,
        lockJson: String?,
        lockHash: String?,
        lock: PluginModuleLock,
    ): ApiPlugin {
        // updateById ignores null fields; the nullable columns below must be cleared on an
        // upgrade (a previous load error, an old schema/lock) and are set explicitly here.
        val existing = get(descriptor.pluginId)
        if (existing != null && (existing.versionCode ?: 0) > descriptor.versionCode) {
            throw PluginVersionConflictException(
                "Plugin ${descriptor.pluginId} version ${descriptor.versionCode} is older than " +
                    "installed version ${existing.versionCode}",
            )
        }
        val row =
            existing ?: ApiPlugin().apply {
                pluginId = descriptor.pluginId
                enable = 1
                source = "UPLOADED"
                defaultAccessMode = ApiInterface.AccessMode.RESTRICTED
            }
        row.name = descriptor.name
        row.description = descriptor.description
        row.versionName = descriptor.versionName
        row.versionCode = descriptor.versionCode
        row.fileHash = digest
        row.jarName = jarName
        row.signerKeyId = signerKeyId
        row.configSchema = schemaJson
        row.openapi = openapiJson
        row.descriptorFormat = descriptor.format
        row.gatewayApiVersion = descriptor.apiVersion
        row.requiresGateway = descriptor.requiresGateway
        row.requiresGatewayApi = descriptor.requiresGatewayApi
        row.dependencyLock = lockJson
        row.lockHash = lockHash
        row.loadStatus = "LOADED"
        row.loadErrorCode = null
        row.loadError = null

        if (existing == null) {
            saveOrThrowException(PluginInstallException("Cannot persist plugin ${descriptor.pluginId}")) {
                save(row)
            }
        } else {
            updateOrThrowException(PluginInstallException("Cannot update plugin ${descriptor.pluginId}")) {
                updateById(row)
            }
        }
        if (lock.modules.isNotEmpty() && lockHash == null) {
            throw PluginInstallException("Dependency lock has no digest")
        }
        updateOrThrowException {
            update(
                KtUpdateWrapper(ApiPlugin()).apply {
                    eq(ApiPlugin::id, row.id)
                    set(ApiPlugin::configSchema, schemaJson)
                    set(ApiPlugin::openapi, openapiJson)
                    set(ApiPlugin::dependencyLock, lockJson)
                    set(ApiPlugin::lockHash, lockHash)
                    set(ApiPlugin::loadError, null as String?)
                    set(ApiPlugin::loadErrorCode, null as String?)
                    set(ApiPlugin::updateTime, LocalDateTime.now())
                },
            )
        }
        return row
    }

    private fun updateLoadState(row: ApiPlugin, status: String, code: String?, error: String?) {
        updateOrThrowException {
            update(
                KtUpdateWrapper(ApiPlugin()).apply {
                    eq(ApiPlugin::id, row.id)
                    set(ApiPlugin::loadStatus, status)
                    set(ApiPlugin::loadErrorCode, code)
                    set(ApiPlugin::loadError, error)
                    set(ApiPlugin::updateTime, LocalDateTime.now())
                },
            )
        }
    }

    /**
     * Mounts the previously installed version again after a failed upgrade.
     *
     * Runs after the failed transaction has rolled back, so the row read here is the row the
     * gateway had before the attempt.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun restoreRuntime(row: ApiPlugin) {
        val pluginId = row.pluginId ?: return
        try {
            val loaded = runtimeRegistry.load(row)
            endpointRegistrar.register(row, loaded.routes)
            runtimeRegistry.activate(pluginId, loaded)
            updateLoadState(row, "LOADED", null, null)
        } catch (ex: Throwable) {
            logger.error("Cannot restore plugin {} after a failed install", pluginId, ex)
            runCatching {
                updateLoadState(row, "LOAD_ERROR", "STARTUP_ERROR", ex.message?.take(MAX_ERROR_LENGTH))
            }
        }
        refreshCache()
    }

    /**
     * Schedules [restoreRuntime] for the end of a transaction that rolled back.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun scheduleRuntimeRestore(row: ApiPlugin?) {
        if (row == null) {
            return
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            restoreRuntime(row)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        restoreRuntime(row)
                    }
                }
            },
        )
    }

    /**
     * Maps any installation failure onto the exception the API reports it as.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun asInstallFailure(ex: Throwable): Throwable = when (ex) {
        is PluginInstallException,
        is PluginNotTrustedException,
        is PluginSignatureInvalidException,
        is PluginVersionConflictException,
        is PluginPurgeFailedException,
        -> ex

        else -> PluginInstallException("Cannot install plugin: ${ex.message}", ex)
    }

    private fun refreshCache() {
        val interfaces =
            apiInterfaceService
                .list()
                .mapNotNull { interfaceRow -> interfaceRow.code?.let { it to interfaceRow } }
                .toMap()
        val plugins =
            list()
                .mapNotNull { plugin -> plugin.pluginId?.let { it to plugin } }
                .toMap()
        registryCache.replaceAll(interfaces, plugins)
        configSchemaCache.replaceAll(
            plugins.mapValues { (_, plugin) -> parseConfigSchemaOrNull(plugin.configSchema) },
        )
    }

    /**
     * Parses a stored schema for the caches, treating an unreadable one as absent.
     *
     * Install and mount use [PluginConfigSchemaUtil.parseOrThrow] instead; only already-stored
     * rows reach this.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun parseConfigSchemaOrNull(json: String?): PluginConfigSchema? =
        runCatching { PluginConfigSchemaUtil.parse(json) }
            .onFailure { logger.warn("Ignoring an unreadable stored plugin config schema", it) }
            .getOrNull()

    private companion object {
        const val MAX_ERROR_LENGTH = 512
    }
}
