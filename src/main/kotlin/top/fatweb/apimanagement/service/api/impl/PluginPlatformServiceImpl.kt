package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import top.fatweb.apimanagement.component.plugin.PluginConfigFieldType
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaCache
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
import top.fatweb.apimanagement.component.plugin.PluginConfigValueAction
import top.fatweb.apimanagement.component.plugin.PluginDatasourceFactory
import top.fatweb.apimanagement.component.plugin.PluginInFlightTracker
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.component.plugin.PluginSettingReader
import top.fatweb.apimanagement.converter.api.toGroupVo
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.component.plugin.runtime.PluginCapabilityFactory
import top.fatweb.apimanagement.component.plugin.runtime.PluginEventBus
import top.fatweb.apimanagement.component.plugin.runtime.PluginOpenApiBuilder
import top.fatweb.apimanagement.component.plugin.runtime.PluginScheduler
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateStatusParam
import top.fatweb.apimanagement.param.system.api.ApiPluginConfigUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLockParser
import top.fatweb.apimanagement.plugin.runtime.route.PluginRouteRegistry
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute
import top.fatweb.apimanagement.plugin.runtime.runtime.LoadedPlugin
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginDescriptor
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginDescriptorParser
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginRuntimeLoader
import top.fatweb.apimanagement.plugin.signing.PluginSigner
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiInterfaceService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import top.fatweb.apimanagement.service.api.IPluginModuleService
import top.fatweb.apimanagement.service.api.IPluginPlatformService
import top.fatweb.apimanagement.service.api.PluginModuleUpload
import top.fatweb.apimanagement.service.permission.IMenuService
import top.fatweb.apimanagement.service.permission.IOperationService
import top.fatweb.apimanagement.service.permission.IPowerService
import top.fatweb.apimanagement.service.permission.IScopeService
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
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

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
    private val runtimeLoader: PluginRuntimeLoader,
    private val routeRegistry: PluginRouteRegistry,
    private val moduleService: IPluginModuleService,
    private val capabilityFactory: PluginCapabilityFactory,
    private val openApiBuilder: PluginOpenApiBuilder,
    private val eventBus: PluginEventBus,
    private val scheduler: PluginScheduler,
    private val configSchemaCache: PluginConfigSchemaCache,
    private val registryCache: PluginRegistryCache,
    private val pluginSettingReader: PluginSettingReader,
    private val inFlightTracker: PluginInFlightTracker,
    private val datasourceFactory: PluginDatasourceFactory,
    private val settingService: IApiPluginSettingService,
    private val trustKeyService: IApiPluginTrustKeyService,
    private val fileStorageProvider: FileStorageProvider,
    private val apiInterfaceService: IApiInterfaceService,
    private val menuService: IMenuService,
    private val scopeService: IScopeService,
    private val operationService: IOperationService,
    private val powerService: IPowerService
) : ServiceImpl<ApiPluginMapper, ApiPlugin>(), IPluginPlatformService, ApplicationRunner {
    private val runtimes = ConcurrentHashMap<String, LoadedPlugin>()
    private val descriptorParser = PluginDescriptorParser()
    private val lockParser = PluginModuleLockParser()

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
     * @param modules dependency modules uploaded with the jar
     * @return installed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun install(
        jarBytes: ByteArray,
        jarName: String,
        modules: List<PluginModuleUpload>
    ): ApiPlugin {
        if (jarBytes.size > serverProperties.storage.pluginJarMaxBytes) {
            throw PluginInstallException(
                "Plugin jar is ${jarBytes.size} bytes, limit is ${serverProperties.storage.pluginJarMaxBytes}"
            )
        }

        val digest = sha256(jarBytes)
        val jarPath = jarPath(digest)
        Files.createDirectories(jarPath.parent)
        Files.write(jarPath, jarBytes)

        var pluginId: String? = null
        var modulesInstalled = false
        try {
            val signerKeyId = verifySignature(jarPath)
            val descriptor = readDescriptor(jarPath)
            pluginId = descriptor.pluginId
            val previousRow = get(descriptor.pluginId)
            if (runtimes.containsKey(descriptor.pluginId)) {
                // Taking the current runtime down first keeps its class loader, subscriptions
                // and module references from outliving the swap. A failure below leaves the row
                // pointing at the replaced jar, which the next startup reloads. The replaced
                // version is an uninstall, so its onUninstall hook runs.
                unloadRuntime(descriptor.pluginId, purge = false, uninstalling = true)
                moduleService.release(descriptor.pluginId)
            }
            val lockBytes = readEntry(jarPath, LOCK_ENTRY)
            val lock = lockBytes?.let(lockParser::parse) ?: PluginModuleLock()
            if (descriptor.dependencies.isNotEmpty() && lock.modules.isEmpty()) {
                throw PluginInstallException(
                    "Plugin ${descriptor.pluginId} declares dependencies but has no dependency lock"
                )
            }

            val schemaJson = readEntry(jarPath, CONFIG_ENTRY)?.toString(Charsets.UTF_8)
            val schema = parseConfigSchemaOrNull(schemaJson)
            configSchemaCache.put(descriptor.pluginId, schema)

            val uploaded = modules.associate { it.fileName.removeSuffix(".jar") to it.bytes }
            moduleService.installForPlugin(descriptor.pluginId, lock) { module ->
                uploaded[module.sha256]
                    ?: moduleService.store().path(module.sha256)
                        .takeIf { Files.isRegularFile(it) }
                        ?.let { Files.readAllBytes(it) }
            }
            modulesInstalled = true

            val loaded = runtimeLoader.load(jarPath, capabilityFactory.create(descriptor.pluginId))
            runtimes[descriptor.pluginId] = loaded
            val row = upsertPluginRow(
                descriptor = descriptor,
                digest = digest,
                jarName = jarName,
                signerKeyId = signerKeyId,
                schemaJson = schemaJson,
                openapiJson = openApiBuilder.build(descriptor.pluginId, loaded.routes),
                lockJson = lockBytes?.toString(Charsets.UTF_8),
                lockHash = lockBytes?.let(::sha256),
                lock = lock
            )
            registerEndpoints(row, loaded)
            routeRegistry.replace(descriptor.pluginId, loaded.routes)
            if (previousRow?.fileHash != null && previousRow.fileHash != digest) {
                runCatching { Files.deleteIfExists(jarPath(previousRow.fileHash!!)) }
            }
            refreshCache()
            return row
        } catch (ex: Throwable) {
            pluginId?.let { id ->
                unloadRuntime(id, purge = false, uninstalling = false)
                if (modulesInstalled) {
                    runCatching { moduleService.release(id) }
                }
            }
            runCatching { Files.deleteIfExists(jarPath) }
            throw if (ex is PluginInstallException) {
                ex
            } else {
                PluginInstallException("Cannot install plugin: ${ex.message}")
            }
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
        val row = get(pluginId) ?: return
        unloadRuntime(pluginId, purge = purgeData, uninstalling = true)
        moduleService.release(pluginId)

        apiInterfaceService.remove(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
        )
        cleanupPermissionTree(pluginId)
        row.id?.let { removeById(it) }
        configSchemaCache.put(pluginId, null)

        if (purgeData) {
            settingService.deleteByPlugin(pluginId)
            datasourceFactory.removeData(pluginId)
            runCatching {
                fileStorageProvider.deleteAtPrefix(StorageKeyUtil.pluginBaseKey(pluginId))
            }
        }
        row.fileHash?.let { hash -> runCatching { Files.deleteIfExists(jarPath(hash)) } }
        refreshCache()
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
        val row = get(pluginId) ?: throw PluginInstallException("Plugin not found: $pluginId")

        if (enabled) {
            val runtime = runtimes[pluginId] ?: loadFromRow(row).also { runtimes[pluginId] = it }
            routeRegistry.replace(pluginId, runtime.routes)
            row.enable = 1
        } else {
            // A disabled plugin does not run at all: its hooks, timers and event subscriptions
            // are taken down with the routes. Module references stay, so enabling it again
            // reloads the same modules without a re-upload.
            unloadRuntime(pluginId, purge = false, uninstalling = false)
            row.enable = 0
        }
        updateOrThrowException { updateById(row) }
        updateLoadState(row, if (enabled) "LOADED" else "DISABLED", null, null)
        refreshCache()
    }

    /**
     * Reloads a plugin from its stored jar.
     *
     * @param pluginId plugin ID
     * @return refreshed plugin row
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun reload(pluginId: String): ApiPlugin {
        val row = get(pluginId) ?: throw PluginInstallException("Plugin not found: $pluginId")
        unloadRuntime(pluginId, purge = false, uninstalling = false)
        val loaded = try {
            loadFromRow(row)
        } catch (ex: Throwable) {
            updateLoadState(row, "LOAD_ERROR", "STARTUP_ERROR", ex.message?.take(512))
            refreshCache()
            throw PluginInstallException("Cannot reload plugin $pluginId: ${ex.message}")
        }
        runtimes[pluginId] = loaded
        routeRegistry.replace(pluginId, loaded.routes)
        row.openapi = openApiBuilder.build(pluginId, loaded.routes)
        updateOrThrowException { updateById(row) }
        updateLoadState(row, "LOADED", null, null)
        refreshCache()
        return row
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
        val wrapper = KtQueryWrapper(ApiPlugin()).apply {
            param?.searchName?.let { like(ApiPlugin::name, it) }
            param?.enable?.let { eq(ApiPlugin::enable, if (it) 1 else 0) }
        }
        return page(page, wrapper).toVoPage()
    }

    override fun getPlugin(pluginId: String): ApiPluginVo =
        get(pluginId)?.toVo() ?: throw NoRecordFoundException()

    @Transactional
    override fun updatePlugin(param: ApiPluginUpdateParam) {
        val existing = param.id?.let(::getById) ?: throw NoRecordFoundException()
        val pluginId = existing.pluginId ?: throw NoRecordFoundException()
        val wasEnabled = existing.enable == 1

        update(
            KtUpdateWrapper(ApiPlugin()).apply {
                eq(ApiPlugin::id, param.id)
                param.enable?.let { set(ApiPlugin::enable, if (it) 1 else 0) }
                set(ApiPlugin::defaultPrice, param.defaultPrice)
                set(ApiPlugin::defaultRateLimit, param.defaultRateLimit)
                set(ApiPlugin::defaultAccessMode, param.defaultAccessMode)
                set(ApiPlugin::updateTime, LocalDateTime.now())
            }
        )

        val nowEnabled = param.enable ?: return
        if (nowEnabled != wasEnabled) {
            setEnabled(pluginId, nowEnabled)
        }
        refreshCache()
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

        val pluginIds = apiInterfaceService.listObjs<Any>(
            QueryWrapper<ApiInterface>().select("DISTINCT plugin_id").apply {
                param?.searchCode?.let { like("code", it) }
                param?.searchName?.let { like("name", it) }
                param?.pluginId?.let { eq("plugin_id", it) }
                param?.enable?.let { eq("enable", if (it) 1 else 0) }
            }
        ).mapNotNull { it?.toString() }.filter { it.isNotBlank() }
        if (pluginIds.isEmpty()) {
            return PageVo(total = 0, pages = 0, size = size, current = current, records = emptyList())
        }

        val pluginById = list(
            KtQueryWrapper(ApiPlugin()).`in`(ApiPlugin::pluginId, pluginIds)
        ).associateBy { it.pluginId }
        val orderedPluginIds = pluginIds.sortedWith(
            compareByDescending<String> { id -> pluginById[id]?.createTime }.thenByDescending { it }
        )
        val total = orderedPluginIds.size.toLong()
        val pages = (total + size - 1) / size
        val from = ((current - 1) * size).coerceAtLeast(0).toInt()
        val pagePluginIds = orderedPluginIds.drop(from).take(size.toInt())
        if (pagePluginIds.isEmpty()) {
            return PageVo(total = total, pages = pages, size = size, current = current, records = emptyList())
        }

        val interfaces = apiInterfaceService.list(
            KtQueryWrapper(ApiInterface()).apply { addInterfaceFilter() }
                .`in`(ApiInterface::pluginId, pagePluginIds)
        ).sortedWith(compareByDescending<ApiInterface> { it.createTime }.thenByDescending { it.id })

        val interfacesByPlugin = interfaces.groupBy { it.pluginId ?: "" }
        val records = pagePluginIds.map { pluginId ->
            ApiGroupVo(
                pluginId = pluginId,
                pluginName = pluginById[pluginId]?.name,
                pluginDescription = pluginById[pluginId]?.description,
                interfaces = interfacesByPlugin[pluginId].orEmpty().map(ApiInterface::toVo)
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
                    set(ApiInterface::needKey, param.needKey)
                    set(ApiInterface::rateLimit, param.rateLimit)
                    set(ApiInterface::enable, param.enable)
                    set(ApiInterface::accessMode, param.accessMode)
                    set(ApiInterface::updateTime, LocalDateTime.now())
                }
            )
        }
        refreshCache()
    }

    override fun updateInterfaceStatus(param: ApiInterfaceUpdateStatusParam) {
        updateOrThrowException(NoRecordFoundException()) {
            apiInterfaceService.update(
                KtUpdateWrapper(ApiInterface()).apply {
                    eq(ApiInterface::id, param.id)
                    set(ApiInterface::enable, if (param.enable == true) 1 else 0)
                    set(ApiInterface::updateTime, LocalDateTime.now())
                }
            )
        }
        refreshCache()
    }

    override fun getPluginConfig(pluginId: String): ApiPluginConfigVo {
        val schema = configSchemaOf(pluginId)
        val stored = settingService.listByPlugin(pluginId)
        val unreadable = schema?.secretKeys().orEmpty()
            .filterNot { pluginSettingReader.isReadable(pluginId, it) }
            .toSet()

        return ApiPluginConfigVo(
            pluginId = pluginId,
            groups = schema?.toGroupVo(stored, unreadable) ?: emptyList(),
            datasources = schema?.datasources?.map { declaration ->
                declaration.toVo(
                    configured = PluginDatasourceUtil.isConfigured(declaration) { key ->
                        stored[key] ?: schema.fieldOf(key)?.default
                    }
                )
            }.orEmpty()
        )
    }

    @Transactional
    override fun updatePluginConfig(param: ApiPluginConfigUpdateParam) {
        val pluginId = param.pluginId!!
        val schema = configSchemaOf(pluginId)
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
                            }
                        )
                    }
                }
            }
        }

        if (datasourceChanged || down) {
            onCommitted { runCatching { reload(pluginId) } }
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
        val duplicated = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicated.isEmpty()) { "$what more than once: ${duplicated.joinToString()}" }
    }

    private fun onCommitted(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = action()
            })
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
            if (row.enable != 1) {
                updateLoadState(row, "DISABLED", null, null)
                return@forEach
            }
            if (runtimes.containsKey(pluginId)) {
                return@forEach
            }
            try {
                val loaded = loadFromRow(row)
                runtimes[pluginId] = loaded
                routeRegistry.replace(pluginId, loaded.routes)
                row.openapi = openApiBuilder.build(pluginId, loaded.routes)
                updateOrThrowException { updateById(row) }
                updateLoadState(row, "LOADED", null, null)
            } catch (ex: Throwable) {
                updateLoadState(row, "LOAD_ERROR", "STARTUP_ERROR", ex.message?.take(512))
            }
        }
        refreshCache()
    }

    private fun loadFromRow(row: ApiPlugin): LoadedPlugin {
        val pluginId = row.pluginId ?: throw PluginInstallException("Plugin row has no pluginId")
        val hash = row.fileHash ?: throw PluginInstallException("Plugin $pluginId has no stored jar")
        val path = jarPath(hash)
        if (!Files.isRegularFile(path)) {
            throw PluginInstallException("Plugin jar is missing: $path")
        }
        configSchemaCache.put(pluginId, parseConfigSchemaOrNull(row.configSchema))
        return runtimeLoader.load(path, capabilityFactory.create(pluginId))
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
        lock: PluginModuleLock
    ): ApiPlugin {
        // updateById ignores null fields; the nullable columns below must be cleared on an
        // upgrade (a previous load error, an old schema/lock) and are set explicitly here.
        val existing = get(descriptor.pluginId)
        if (existing != null && (existing.versionCode ?: 0) > descriptor.versionCode) {
            throw PluginInstallException(
                "Plugin ${descriptor.pluginId} version ${descriptor.versionCode} is older than " +
                    "installed version ${existing.versionCode}"
            )
        }
        val row = existing ?: ApiPlugin().apply {
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
                }
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
                }
            )
        }
    }

    private fun registerEndpoints(row: ApiPlugin, loaded: LoadedPlugin) {
        val pluginId = row.pluginId ?: return
        val menu = ensureMenu(pluginId, row.name.orEmpty())
        val routesByCode = loaded.routes.associateBy { it.apiCode }
        val previousByCode = apiInterfaceService.list(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
        ).mapNotNull { interfaceRow -> interfaceRow.code?.let { code -> code to interfaceRow } }
            .toMap()

        (previousByCode.keys - routesByCode.keys).forEach { orphan ->
            previousByCode.getValue(orphan).id?.let { apiInterfaceService.removeById(it) }
        }

        previousByCode.forEach { (code, interfaceRow) ->
            val route = routesByCode[code] ?: return@forEach
            val path = fullPath(pluginId, route)
            if (interfaceRow.path != path || interfaceRow.method != route.definition.method) {
                updateOrThrowException {
                    apiInterfaceService.update(
                        KtUpdateWrapper(ApiInterface()).apply {
                            eq(ApiInterface::id, interfaceRow.id)
                            set(ApiInterface::path, "#moving:${interfaceRow.id}")
                        }
                    )
                }
            }
        }

        val scopeIds = mutableMapOf<Int, Long>()
        routesByCode.values.forEach { route ->
            val code = route.apiCode
            val name = route.definition.operation.summary
                .ifBlank { route.definition.operation.operationId }
            val path = fullPath(pluginId, route)
            val existing = previousByCode[code]
            val entity = existing ?: ApiInterface().apply {
                this.pluginId = pluginId
                this.code = code
                this.billingMode = ApiInterface.BillingMode.SUCCESS_ONLY
                this.needKey = 1
                this.enable = 1
            }
            entity.name = name
            entity.description = route.definition.operation.description
            entity.path = path
            entity.method = route.definition.method
            entity.apiVersion = route.definition.apiVersion
            if (existing == null) {
                saveOrThrowException { apiInterfaceService.save(entity) }
            } else {
                updateOrThrowException { apiInterfaceService.updateById(entity) }
            }

            val scopeId = scopeIds.getOrPut(route.definition.apiVersion) {
                ensureScope("v${route.definition.apiVersion}", menu.id!!).id!!
            }
            upsertOperation(code, name, scopeId)
        }

        (previousByCode.keys - routesByCode.keys).forEach { code ->
            operationService.getOne(KtQueryWrapper(Operation()).eq(Operation::code, code))?.let { operation ->
                operation.id?.let { powerService.removeById(it) }
                operation.id?.let { operationService.removeById(it) }
            }
        }
    }

    private fun fullPath(pluginId: String, route: RegisteredRoute): String =
        "/api/$pluginId/v${route.definition.apiVersion}${route.definition.path}"

    private fun ensureMenu(pluginId: String, name: String): Menu {
        menuService.getOne(KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId))?.let { return it }
        val menu = Menu().apply {
            this.pluginId = pluginId
            this.name = name
            this.parentId = API_ROOT_MENU_ID
            this.moduleId = API_MODULE_ID
        }
        saveOrThrowException(PluginInstallException("Cannot create menu for plugin $pluginId")) {
            menuService.save(menu)
        }
        menu.id?.let { id ->
            saveOrThrowException {
                powerService.save(Power().apply { this.id = id; this.typeId = POWER_TYPE_MENU })
            }
        }
        return menu
    }

    private fun ensureScope(name: String, menuId: Long): Scope {
        scopeService.getOne(
            KtQueryWrapper(Scope()).eq(Scope::name, name).eq(Scope::menuId, menuId)
        )?.let { return it }
        val scope = Scope().apply {
            this.name = name
            this.menuId = menuId
        }
        saveOrThrowException(PluginInstallException("Cannot create scope $name")) {
            scopeService.save(scope)
        }
        scope.id?.let { id ->
            saveOrThrowException {
                powerService.save(Power().apply { this.id = id; this.typeId = POWER_TYPE_SCOPE })
            }
        }
        return scope
    }

    private fun upsertOperation(code: String, name: String, scopeId: Long) {
        val existing = operationService.getOne(KtQueryWrapper(Operation()).eq(Operation::code, code))
        if (existing == null) {
            val operation = Operation().apply {
                this.code = code
                this.name = name
                this.scopeId = scopeId
            }
            saveOrThrowException(PluginInstallException("Cannot create operation $code")) {
                operationService.save(operation)
            }
            operation.id?.let { id ->
                saveOrThrowException {
                    powerService.save(Power().apply { this.id = id; this.typeId = POWER_TYPE_OPERATION })
                }
            }
        } else {
            existing.name = name
            existing.scopeId = scopeId
            updateOrThrowException { operationService.updateById(existing) }
        }
    }

    private fun cleanupPermissionTree(pluginId: String) {
        val menu = menuService.getOne(KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId)) ?: return
        val menuId = menu.id ?: return
        val scopes = scopeService.list(KtQueryWrapper(Scope()).eq(Scope::menuId, menuId))
        val scopeIds = scopes.mapNotNull { it.id }
        val operations = if (scopeIds.isEmpty()) {
            emptyList()
        } else {
            operationService.list(KtQueryWrapper(Operation()).`in`(Operation::scopeId, scopeIds))
        }
        operations.forEach { operation ->
            operation.id?.let { powerService.removeById(it) }
            operation.id?.let { operationService.removeById(it) }
        }
        scopes.forEach { scope ->
            scope.id?.let { powerService.removeById(it) }
            scope.id?.let { scopeService.removeById(it) }
        }
        powerService.removeById(menuId)
        menuService.removeById(menuId)
    }

    private fun unloadRuntime(pluginId: String, purge: Boolean, uninstalling: Boolean) {
        inFlightTracker.beginDrain(pluginId)
        routeRegistry.unregisterPlugin(pluginId)
        eventBus.removePlugin(pluginId)
        scheduler.removePlugin(pluginId)
        inFlightTracker.awaitDrain(pluginId, IN_FLIGHT_DRAIN_TIMEOUT_MILLIS)
        val loaded = runtimes.remove(pluginId)
        if (loaded != null) {
            if (purge) {
                runCatching { loaded.purge() }
            }
            runCatching { loaded.stop() }
            if (uninstalling) {
                runCatching { loaded.uninstall() }
            }
            runCatching { loaded.close() }
        }
        inFlightTracker.release(pluginId)
    }

    private fun refreshCache() {
        val interfaces = apiInterfaceService.list()
            .mapNotNull { interfaceRow -> interfaceRow.code?.let { it to interfaceRow } }
            .toMap()
        val plugins = list()
            .mapNotNull { plugin -> plugin.pluginId?.let { it to plugin } }
            .toMap()
        registryCache.replaceAll(interfaces, plugins)
        configSchemaCache.replaceAll(
            plugins.mapValues { (_, plugin) -> parseConfigSchemaOrNull(plugin.configSchema) }
        )
    }

    private fun verifySignature(jar: Path): String {
        val publicKey = PluginSigner.readPublicKey(jar)
            ?: throw PluginInstallException("Plugin is not signed")
        if (!PluginSigner.verify(jar, publicKey)) {
            throw PluginInstallException("Plugin signature is invalid")
        }
        val keyId = PluginSigner.spkiFingerprint(publicKey)
        val trustKey = trustKeyService.getByKeyId(keyId)
            ?: throw PluginInstallException("The plugin signer is not trusted: $keyId")
        if (trustKey.enable != 1) {
            throw PluginInstallException("The plugin signer key is disabled: $keyId")
        }
        return keyId
    }

    private fun readDescriptor(jar: Path): PluginDescriptor {
        val bytes = readEntry(jar, DESCRIPTOR_ENTRY)
            ?: throw PluginInstallException("Plugin jar has no $DESCRIPTOR_ENTRY")
        return try {
            descriptorParser.parse(bytes)
        } catch (ex: Exception) {
            throw PluginInstallException("Invalid plugin descriptor: ${ex.message}")
        }
    }

    private fun readEntry(jar: Path, name: String): ByteArray? =
        JarFile(jar.toFile()).use { file ->
            file.getJarEntry(name)?.let { entry -> file.getInputStream(entry).readBytes() }
        }

    private fun parseConfigSchemaOrNull(json: String?): PluginConfigSchema? =
        runCatching { PluginConfigSchemaUtil.parse(json) }.getOrNull()

    private fun jarPath(digest: String): Path =
        Path.of(serverProperties.storage.pluginDir).resolve("$digest.jar")

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val DESCRIPTOR_ENTRY = "META-INF/api-plugin.json"
        const val LOCK_ENTRY = "META-INF/api-plugin-deps.json"
        const val CONFIG_ENTRY = "META-INF/plugin-config.json"

        const val API_MODULE_ID = 2000000L
        const val API_ROOT_MENU_ID = 2990000L
        const val POWER_TYPE_MENU = 2
        const val POWER_TYPE_SCOPE = 3
        const val POWER_TYPE_OPERATION = 4

        const val IN_FLIGHT_DRAIN_TIMEOUT_MILLIS = 10_000L
    }
}
