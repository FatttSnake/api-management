package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import jakarta.annotation.PreDestroy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.component.api.ApiVersionCondition
import top.fatweb.apimanagement.component.plugin.*
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.converter.api.toEntity
import top.fatweb.apimanagement.converter.api.toGroupVo
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.exception.*
import top.fatweb.apimanagement.mapper.api.ApiInterfaceMapper
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
import top.fatweb.apimanagement.mapper.permission.MenuMapper
import top.fatweb.apimanagement.mapper.permission.OperationMapper
import top.fatweb.apimanagement.mapper.permission.PowerMapper
import top.fatweb.apimanagement.mapper.permission.ScopeMapper
import top.fatweb.apimanagement.param.system.api.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.annotation.ApiController
import top.fatweb.apimanagement.sdk.plugin.*
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigVo
import top.fatweb.apimanagement.vo.api.ApiPluginVo
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import java.util.jar.JarFile
import javax.sql.DataSource
import io.swagger.v3.oas.annotations.Operation as SwaggerOperation

/**
 * API plugin service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JsonMapper
 * @see ApplicationContext
 * @see ThreadPoolTaskExecutor
 * @see RequestMappingHandlerMapping
 * @see ServerProperties
 * @see FileStorageProvider
 * @see PowerMapper
 * @see MenuMapper
 * @see ScopeMapper
 * @see OperationMapper
 * @see ApiInterfaceMapper
 * @see IStorageBlobService
 * @see IApiPluginSettingService
 * @see IApiPluginTrustKeyService
 * @see IApiAccountService
 * @see PluginDatasourceFactory
 * @see PluginStorageFactory
 * @see PluginRegistryCache
 * @see PluginConfigSchemaCache
 * @see PluginSettingReader
 * @see PluginOpenApiGenerator
 * @see ServiceImpl
 * @see ApiPluginMapper
 * @see ApiPlugin
 * @see IApiPluginService
 * @see ApplicationRunner
 */
@Service
@DS("master")
class ApiPluginServiceImpl(
    private val objectMapper: JsonMapper,
    @param:Qualifier("pluginRemountExecutor") private val remountExecutor: ThreadPoolTaskExecutor,
    private val applicationContext: ApplicationContext,
    @Lazy private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val serverProperties: ServerProperties,
    private val fileStorageProvider: FileStorageProvider,
    private val powerMapper: PowerMapper,
    private val menuMapper: MenuMapper,
    private val scopeMapper: ScopeMapper,
    private val operationMapper: OperationMapper,
    private val apiInterfaceMapper: ApiInterfaceMapper,
    private val storageBlobService: IStorageBlobService,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val apiPluginTrustKeyService: IApiPluginTrustKeyService,
    @Lazy private val apiAccountService: IApiAccountService,
    private val pluginDatasourceFactory: PluginDatasourceFactory,
    private val pluginStorageFactory: PluginStorageFactory,
    private val pluginRegistryCache: PluginRegistryCache,
    private val pluginConfigSchemaCache: PluginConfigSchemaCache,
    private val pluginInFlightTracker: PluginInFlightTracker,
    private val pluginSettingReader: PluginSettingReader,
    private val pluginOpenApiGenerator: PluginOpenApiGenerator,
    private val pluginLibraryCache: PluginLibraryCache,
    private val pluginLibraryRegistry: PluginLibraryRegistry
) : ServiceImpl<ApiPluginMapper, ApiPlugin>(), IApiPluginService,
    ApplicationRunner {
    companion object {
        /**
         * Module ID of the API module (t_s_module)
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val API_MODULE_ID = 2000000L

        /**
         * Root menu ID of the API module (t_s_menu)
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val API_ROOT_MENU_ID = 2990000L

        /**
         * Type ID of menu power
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val POWER_TYPE_MENU = 2

        /**
         * Type ID of scope power
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val POWER_TYPE_SCOPE = 3

        /**
         * Type ID of operation power
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val POWER_TYPE_OPERATION = 4

        /**
         * How long an unmount waits for the plugin's in-flight requests to finish
         *
         * When it expires, the plugin's resources are closed under a request that is still running.
         */
        private const val IN_FLIGHT_DRAIN_TIMEOUT_MILLIS = 10_000L

        /**
         * Jar entry path of the plugin descriptor
         */
        private const val DESCRIPTOR_ENTRY = "META-INF/api-plugin.json"

        /**
         * Jar entry path of the declared plugin config schema
         */
        private const val CONFIG_ENTRY = "META-INF/plugin-config.json"

        /**
         * Maximum length of an API scoping code
         *
         * Must match the `varchar(200)` columns the code is persisted in.
         */
        private const val MAX_CODE_LENGTH = 200

        /**
         * Build the endpoint's persisted identity code from the plugin ID, its API version and
         * the method's `@Operation`
         *
         * @param pluginId Plugin ID, from the descriptor or the matched URL
         * @param version API version the controller declares
         * @param method Endpoint method
         * @return API scoping code
         * @throws MissingOperationIdException when the method declares no non-blank
         *         `@Operation.operationId`. A mount refuses such a method earlier - see
         *         [mountEndpoints] - so this is reachable only from the request path, as a state
         *         the gateway never produced, and it is raised rather than answered with a code
         *         that is not the endpoint's
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ApiController
         */
        fun buildCode(pluginId: String, version: Int, method: Method): String {
            val operationId = method.getAnnotation(SwaggerOperation::class.java)?.operationId
                ?.takeIf { it.isNotBlank() }
                ?: throw MissingOperationIdException(
                    "Endpoint ${method.declaringClass.name}#${method.name} declares no non-blank " +
                            "@Operation(operationId = \"...\"), which is its persisted identity"
                )
            return "api:$pluginId:v$version:$operationId"
        }
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Serializes the mount lifecycle of every plugin
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val mountLock = Any()

    /**
     * Plugin IDs with a remount owed to a committed configuration change
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val pendingRemounts: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val pluginRuntimes = ConcurrentHashMap<String, PluginRuntime>()

    /**
     * Whether the gateway is shutting down
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Volatile
    private var shuttingDown = false

    override fun run(args: ApplicationArguments) {
        try {
            loadUploadedPlugins()
        } catch (e: Exception) {
            if (e is IllegalArgumentException || e is IllegalStateException) {
                throw e
            }
            logger.warn("Failed to load uploaded plugins: {}", e.message)
        } finally {
            // Populated even when a plugin failed to mount: the administrator still has to
            // see it, and the configuration it declares is what explains the failure
            refreshCache()
        }
    }

    /**
     * Stop every plugin as the gateway stops
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @PreDestroy
    fun onShutdown() {
        shuttingDown = true
        // Already stopped in the ordinary path, since SmartLifecycle beans go first; kept
        // because it costs nothing and covers a close that never got as far as stopping it
        runCatching { remountExecutor.shutdown() }

        val mounted = pluginRuntimes.keys.toList()
        if (mounted.isEmpty()) {
            logger.debug("No plugin was mounted at shutdown")
        } else {
            logger.info("Stopping {} plugin(s) at shutdown: {}", mounted.size, mounted.joinToString())
        }

        mounted.forEach { pluginId ->
            runCatching {
                pluginRuntimes.remove(pluginId)?.let { runtime ->
                    synchronized(mountLock) { unmount(runtime, deleteJar = false) }
                }
            }.onFailure {
                logger.warn("Failed to stop plugin '{}' at shutdown: {}", pluginId, it.message)
            }
        }
    }

    override fun getPluginPage(apiPluginGetParam: ApiPluginGetParam?): PageVo<ApiPluginVo> {
        val page = Page<ApiPlugin>(apiPluginGetParam?.currentPage ?: 1, apiPluginGetParam?.pageSize ?: 20)
        setPageSort(apiPluginGetParam, page, OrderItem.desc("create_time"))

        val wrapper = KtQueryWrapper(ApiPlugin()).apply {
            apiPluginGetParam?.searchName?.let { like(ApiPlugin::name, it) }
            apiPluginGetParam?.enable?.let { eq(ApiPlugin::enable, if (it) 1 else 0) }
        }

        return page(page, wrapper).toVoPage()
    }

    override fun getPlugin(pluginId: String): ApiPluginVo =
        getByPluginId(pluginId)?.toVo() ?: throw NoRecordFoundException()

    override fun installPlugin(jarBytes: ByteArray, jarName: String): ApiPluginVo = synchronized(mountLock) {
        val maxJarBytes = serverProperties.storage.pluginJarMaxBytes
        if (jarBytes.size > maxJarBytes) {
            throw PluginInstallException("Plugin jar exceeds ${maxJarBytes / 1024 / 1024} MB")
        }
        val pluginDir = Path.of(serverProperties.storage.pluginDir)
        Files.createDirectories(pluginDir)
        val fileHash = jarBytes.sha256HexString()
        val jarPath = pluginDir.resolve("$fileHash.jar")
        Files.write(jarPath, jarBytes)

        doInstall(jarPath, jarName, persistBlob = true, checkVersion = true)
    }

    override fun updatePlugin(apiPluginUpdateParam: ApiPluginUpdateParam) = synchronized(mountLock) {
        val existing = apiPluginUpdateParam.id?.let { getById(it) } ?: throw NoRecordFoundException()
        val pluginId = existing.pluginId ?: throw NoRecordFoundException()
        val wasEnabled = existing.enable == 1

        updateOrThrowException {
            update(
                KtUpdateWrapper(ApiPlugin()).apply {
                    eq(ApiPlugin::id, apiPluginUpdateParam.id)
                    set(ApiPlugin::enable, apiPluginUpdateParam.enable)
                    set(ApiPlugin::defaultPrice, apiPluginUpdateParam.defaultPrice)
                    set(ApiPlugin::defaultRateLimit, apiPluginUpdateParam.defaultRateLimit)
                    set(ApiPlugin::defaultAccessMode, apiPluginUpdateParam.defaultAccessMode)
                }
            )
        }
        refreshCache()

        // The enable flag rides along with the defaults on this endpoint, so the runtime follows
        // it only when the update actually moved it
        val nowEnabled = apiPluginUpdateParam.enable ?: return@synchronized
        if (nowEnabled != wasEnabled) {
            syncEnable(pluginId, nowEnabled)
        }
    }

    override fun updatePluginStatus(apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam) =
        synchronized(mountLock) {
            val existing = apiPluginUpdateStatusParam.id?.let { getById(it) } ?: throw NoRecordFoundException()
            val pluginId = existing.pluginId ?: throw NoRecordFoundException()
            val wasEnabled = existing.enable == 1
            val nowEnabled = apiPluginUpdateStatusParam.enable == true

            updateOrThrowException { updateById(apiPluginUpdateStatusParam.toEntity()) }
            refreshCache()

            if (nowEnabled != wasEnabled) {
                syncEnable(pluginId, nowEnabled)
            }
        }

    /**
     * Bring a plugin's runtime in line with the enable flag that was just written
     *
     * The caller holds [mountLock], and has already written the row and refreshed the cache, so
     * that a request arriving while this runs reads the new intent rather than the old one:
     * switched off is refused, never served from a runtime that is on its way out.
     *
     * Disabling takes the runtime down and nothing else - the plugin's settings, its file area,
     * its datasource configuration and its grants all stay exactly where they are, and switching
     * it back on mounts the same jar over the same rows. Enabling is that mount, and a failure in
     * it is the ordinary failed-mount outcome: the rows survive, `load_error` carries the reason,
     * and the next attempt retries them.
     *
     * @param pluginId Plugin ID
     * @param enabled Whether the plugin is now enabled
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see remount
     * @see unmount
     */
    private fun syncEnable(pluginId: String, enabled: Boolean) {
        if (!enabled) {
            // deleteJar false for the plain reason: the jar is what the row points at, and it is
            // what switching the plugin back on mounts
            pluginRuntimes.remove(pluginId)?.let { unmount(it, deleteJar = false, uninstalling = false) }

            return
        }

        // The flag is the intent and the runtime is the fact: a plugin already up has nothing to
        // gain from being mounted again
        if (pluginRuntimes.containsKey(pluginId)) {
            return
        }

        remount(pluginId)
    }

    override fun getPluginConfig(pluginId: String): ApiPluginConfigVo {
        val schema = configSchemaOf(pluginId)
        val stored = apiPluginSettingService.listByPlugin(pluginId)

            // A stored secret the gateway itself can no longer read: reported rather than raised,
        // because re-entering the value is the administrator's to do and nothing else fixes it
        val unreadable = schema?.secretKeys().orEmpty()
            .filterNot { pluginSettingReader.isReadable(pluginId, it) }
            .toSet()

        return ApiPluginConfigVo(
            pluginId = pluginId,
            groups = schema?.toGroupVo(stored, unreadable) ?: emptyList(),
            // Answered from the values already read rather than by connecting to anything: a
            // datasource is configured exactly when the config says where to connect to, and
            // reporting the state of one the gateway would refuse to use is more useful to
            // the administrator than failing to show the form that fixes it
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
    override fun updatePluginConfig(apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam) {
        val pluginId = apiPluginConfigUpdateParam.pluginId!!
        val schema = configSchemaOf(pluginId)
            ?: throw IllegalArgumentException("Plugin '$pluginId' does not declare any configuration")

        val submitted = submittedGroupsOf(apiPluginConfigUpdateParam)

        // What is stored right now, secrets as plaintext. Read before anything is written: it
        // is both what a key the submission leaves out is checked against, and what "did this
        // change anything" is answered from
        val stored = pluginSettingReader.resolveAll(pluginId)
        PluginConfigSchemaUtil.validate(schema, submitted, stored)

        // What the values read back as once they are written
        val effective = PluginConfigSchemaUtil.effectiveValues(schema, submitted, stored)

        // The gateway's own rules on top of the field constraints, applied before anything is
        // written: a value that commits and only then fails the mount would leave the plugin
        // down with nothing in the response to say why
        schema.datasources.forEach { declaration ->
            PluginDatasourceUtil.resolve(declaration) { key -> effective[key] }
        }

        // Whether the plugin has to be remounted for the values to reach it, as opposed to
        // being visible on its next read. Asked as "did the value this datasource is described
        // by change" rather than "was something submitted": a console submits the fields of the
        // group it was saving, so reacting to the request would restart the plugin over a value
        // that already is the one it is running with - and a secret can only be compared as
        // plaintext, which is why it is read decrypted
        val previous = PluginConfigSchemaUtil.effectiveValues(schema, emptyMap(), stored)
        val datasourceChanged = schema.datasourceKeys.any { previous[it] != effective[it] }

        // A plugin that failed to mount reads as a load error on its row, and every save is a
        // chance that the administrator has just fixed what stopped it. Without this a plugin
        // left down by an unreachable server, or by anything other than a datasource value,
        // could only be brought back by reloading it by hand. Read from the cache, where the
        // row is certain to be - configSchemaOf above resolves through it
        val down = getByPluginId(pluginId)?.loadError != null

        submitted.values.forEach { values ->
            values.forEach { (key, value) ->
                // validate has already read this schema, so the field is there
                val field = schema.fieldOf(key) ?: return@forEach

                when (PluginConfigSchemaUtil.actionOf(field, value)) {
                    // Nothing was decided about this key, so nothing is written for it
                    PluginConfigValueAction.KEEP -> Unit

                    // Clearing removes the row rather than storing a blank, so the plugin reads
                    // the declared default again - a secret has no default, and reads nothing
                    PluginConfigValueAction.CLEAR -> apiPluginSettingService.delete(pluginId, key)

                    PluginConfigValueAction.SET -> {
                        val set = value!!
                        // A secret is stored as ciphertext, which is the only value in the table
                        // that is not the one the plugin reads back
                        apiPluginSettingService.set(
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
            onCommitted { scheduleRemount(pluginId) }
        }
    }

    /**
     * Read a submission as values by group
     *
     * @param apiPluginConfigUpdateParam Update API plugin config parameters
     * @return Submitted values, by config group and then by config key; a null value is a key
     *         that was left out
     * @throws IllegalArgumentException when a group or a key is named more than once
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigGroupParam
     */
    private fun submittedGroupsOf(
        apiPluginConfigUpdateParam: ApiPluginConfigUpdateParam
    ): Map<String, Map<String, String?>> {
        val groups = apiPluginConfigUpdateParam.groups.orEmpty()
        requireDistinct(groups.mapNotNull { it.key }, "Plugin config groups are submitted")

        return groups.associate { group ->
            val groupKey = group.key!!
            val values = group.values.orEmpty()
            requireDistinct(values.mapNotNull { it.key }, "Plugin config group '$groupKey' submits a key")

            groupKey to values.associate { it.key!! to it.value }
        }
    }

    /**
     * Refuse a submission that names the same thing twice
     *
     * @param names Names as submitted
     * @param what What was named, for the error message
     * @throws IllegalArgumentException when a name appears more than once
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun requireDistinct(names: List<String>, what: String) {
        val duplicated = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        require(duplicated.isEmpty()) { "$what more than once: ${duplicated.joinToString()}" }
    }

    override fun uninstallPlugin(pluginId: String, purgeData: Boolean) = synchronized(mountLock) {
        val plugin = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))
            ?: throw PluginInstallException("Plugin not found: $pluginId")
        if (plugin.source != "UPLOADED") {
            throw PluginInstallException("Built-in plugins cannot be uninstalled")
        }

        removePlugin(pluginId, purgeData)
    }

    override fun reloadPlugin(pluginId: String): ApiPluginVo = synchronized(mountLock) {
        val plugin = getByPluginIdOrQuery(pluginId)
            ?: throw PluginInstallException("Plugin not found: $pluginId")
        if (plugin.source != "UPLOADED") {
            throw PluginInstallException("Built-in plugins cannot be reloaded")
        }

        remount(pluginId)
    }

    override fun testPluginDatasource(pluginId: String, name: String, values: Map<String, String?>) {
        // Checked against the installed plugin rather than the schema cache alone, so an
        // unknown plugin is reported as one rather than as a plugin without declaration
        if (getByPluginId(pluginId) == null) {
            throw NoRecordFoundException()
        }

        pluginDatasourceFactory.test(pluginId, name, values)
    }

    override fun getInterfacePage(apiInterfaceGetParam: ApiInterfaceGetParam?): PageVo<ApiGroupVo> {
        val current = apiInterfaceGetParam?.currentPage ?: 1
        val size = apiInterfaceGetParam?.pageSize ?: 20

        fun KtQueryWrapper<ApiInterface>.addInterfaceFilter() {
            apiInterfaceGetParam?.searchCode?.let { like(ApiInterface::code, it) }
            apiInterfaceGetParam?.searchName?.let { like(ApiInterface::name, it) }
            apiInterfaceGetParam?.pluginId?.let { eq(ApiInterface::pluginId, it) }
            apiInterfaceGetParam?.enable?.let { eq(ApiInterface::enable, if (it) 1 else 0) }
        }

        // (1) Plugin groups = distinct plugins owning >= 1 matching interface. DB-side
        //     DISTINCT keeps the in-memory list bounded by plugin count, never interfaces.
        //     KtQueryWrapper only selects typed columns, so use a plain QueryWrapper here.
        val pluginIds = apiInterfaceMapper.selectObjs<Any>(
            QueryWrapper<ApiInterface>().select("DISTINCT plugin_id").apply {
                apiInterfaceGetParam?.searchCode?.let { like("code", it) }
                apiInterfaceGetParam?.searchName?.let { like("name", it) }
                apiInterfaceGetParam?.pluginId?.let { eq("plugin_id", it) }
                apiInterfaceGetParam?.enable?.let { eq("enable", if (it) 1 else 0) }
            }
        ).mapNotNull { it?.toString() }.filter { it.isNotBlank() }
        if (pluginIds.isEmpty()) {
            return PageVo(total = 0, pages = 0, size = size, current = current, records = emptyList())
        }

        val pluginById = list(
            KtQueryWrapper(ApiPlugin()).`in`(ApiPlugin::pluginId, pluginIds)
        ).associateBy { it.pluginId }

        // (2) Plugin groups are ordered by plugin create_time desc; missing plugin rows
        //     (null create_time) fall to the bottom. In-memory paging over this tiny list.
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

        // (3) Interfaces of the current page's plugins only; order within a group by
        //     interface create_time desc (secondary id desc for stability).
        val interfaces = apiInterfaceMapper.selectList(
            KtQueryWrapper(ApiInterface()).apply { addInterfaceFilter() }.`in`(ApiInterface::pluginId, pagePluginIds)
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

    override fun updateInterface(apiInterfaceUpdateParam: ApiInterfaceUpdateParam) {
        updateOrThrowException {
            apiInterfaceMapper.update(
                KtUpdateWrapper(ApiInterface()).apply {
                    eq(ApiInterface::id, apiInterfaceUpdateParam.id)
                    set(ApiInterface::price, apiInterfaceUpdateParam.price)
                    set(ApiInterface::billingMode, apiInterfaceUpdateParam.billingMode)
                    set(ApiInterface::needKey, apiInterfaceUpdateParam.needKey)
                    set(ApiInterface::rateLimit, apiInterfaceUpdateParam.rateLimit)
                    set(ApiInterface::enable, apiInterfaceUpdateParam.enable)
                    set(ApiInterface::accessMode, apiInterfaceUpdateParam.accessMode)
                }
            ) > 0
        }
        refreshCache()
    }

    override fun updateInterfaceStatus(apiInterfaceUpdateStatusParam: ApiInterfaceUpdateStatusParam) {
        updateOrThrowException { apiInterfaceMapper.updateById(apiInterfaceUpdateStatusParam.toEntity()) > 0 }
        refreshCache()
    }

    override fun getByPluginId(pluginId: String): ApiPlugin? = pluginRegistryCache.getByPluginId(pluginId)

    override fun getByCode(code: String): ApiInterface? = pluginRegistryCache.getByCode(code)

    override fun resolveAccessMode(api: ApiInterface): ApiInterface.AccessMode =
        api.accessMode ?: getByPluginId(api.pluginId ?: "")?.defaultAccessMode
        ?: ApiInterface.AccessMode.RESTRICTED

    /**
     * Resolve the declared config schema of a plugin
     *
     * @param pluginId Plugin ID
     * @return Parsed schema, or null when the plugin declares no configuration
     * @throws NoRecordFoundException when no such plugin is installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun configSchemaOf(pluginId: String): PluginConfigSchema? {
        if (getByPluginId(pluginId) == null) {
            throw NoRecordFoundException()
        }

        return pluginConfigSchemaCache.get(pluginId)
    }

    /**
     * Queue a remount of a plugin whose configuration just committed
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun scheduleRemount(pluginId: String) {
        // Nothing is remounted on the way out: the plugin this would mount has just been
        // unmounted, and a mount that lands after shutdown would leave a class loader, a child
        // context and a pool behind that nothing is left to close
        if (shuttingDown) {
            return
        }

        pendingRemounts.add(pluginId)
        // This runs inside a transaction's afterCommit, where a rejected submit has nowhere to
        // be reported, so a closing pool is recorded rather than thrown out of a commit
        runCatching { remountExecutor.execute(::drainRemounts) }
            .onFailure { logger.debug("Remount of plugin '{}' skipped: {}", pluginId, it.message) }
    }

    /**
     * Remount every plugin currently owed one
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun drainRemounts() {
        val iterator = pendingRemounts.iterator()
        while (iterator.hasNext()) {
            val pluginId = iterator.next()
            iterator.remove()

            remountQuietly(pluginId)
        }
    }

    /**
     * Remount a plugin whose configuration changed
     *
     * Nothing here runs on a request thread, so a plugin's lifecycle hooks see no request
     * identity.
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun remountQuietly(pluginId: String) {
        if (shuttingDown) {
            return
        }

        runCatching {
            val plugin = getByPluginIdOrQuery(pluginId) ?: return
            if (plugin.source != "UPLOADED") {
                logger.debug(
                    "Remount after a configuration change skipped for plugin '{}': its source is '{}'",
                    pluginId,
                    plugin.source
                )

                return
            }
            // Saving a configuration is not a reason to bring a plugin back: the remount this
            // was queued for belongs to a plugin that is meant to be running, and one the
            // administrator switched off is not. This is also the second of the two conditions
            // that queue a remount - a plugin left down by a failed mount - so without it a
            // disabled plugin would be mounted again by the first save that followed
            if (plugin.enable != 1) {
                logger.debug(
                    "Remount after a configuration change skipped for plugin '{}': it is disabled",
                    pluginId
                )

                return
            }

            // Read outside the lock because it can throw and is cheap; remount re-reads under
            // it, so an uninstall racing this resolves to "plugin not found" and is caught
            synchronized(mountLock) {
                // Checked again under the lock, and this is the one that matters: a remount
                // queued before shutdown waits here for the unmount that shutdown is running,
                // and would otherwise mount a plugin back after everything had been taken down
                if (shuttingDown) {
                    return
                }

                remount(pluginId)
            }
        }.onFailure {
            logger.warn("Failed to remount plugin '{}' after a configuration change: {}", pluginId, it.message)
        }
    }

    /**
     * Remount an installed plugin from the jar its row points at
     *
     * The caller holds [mountLock].
     *
     * @param pluginId Plugin ID
     * @return Installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see prepareMount
     * @see activateMount
     */
    private fun remount(pluginId: String): ApiPluginVo {
        val plugin = getByPluginIdOrQuery(pluginId)
            ?: throw PluginInstallException("Plugin not found: $pluginId")

        val fileHash = plugin.fileHash?.takeIf { it.isNotBlank() }
            ?: throw PluginInstallException("Plugin has no jar file hash and cannot be reloaded")
        val jarName = plugin.jarName ?: "$pluginId.jar"

        // The blob and the jar it materialises are read before anything is built: a blob that
        // vanished is a failure that costs nothing, and placing the file is what lets the build
        // below read it
        val bytes = storageBlobService.loadFile(fileHash)
            ?: throw PluginInstallException("Plugin jar blob not found for hash $fileHash")
        val pluginDir = Path.of(serverProperties.storage.pluginDir)
        Files.createDirectories(pluginDir)
        val jarPath = pluginDir.resolve("$fileHash.jar")
        Files.write(jarPath, bytes)

        // Nothing below this line has touched the running plugin, and this call has to succeed
        // for it to stay that way: it is the last point at which a failure is free
        val prepared = prepareMount(
            jarPath, jarName, persistBlob = false, checkVersion = false, expectedPluginId = pluginId
        )

        return try {
            // The old runtime must be gone before the new mappings are registered: two live
            // mappings for the same path make the dispatcher reject every request to that path
            // as ambiguous. Its jar survives because that is the very file the new mount read.
            pluginRuntimes.remove(pluginId)?.let { unmount(it, deleteJar = false) }

            activateMount(prepared, preserveRegistration = true)
        } catch (e: Exception) {
            // activateMount has already recorded the failure and refreshed the cache, since it
            // is also the path a failed install takes
            logger.error("Failed to remount plugin '{}': {}", pluginId, e.message, e)

            throw e
        }
    }

    /**
     * Uninstall a plugin, giving it the chance to take its own data with it
     *
     * Replacing a plugin with a newer version does not come through here: that leaves the
     * registration standing and only swaps the runtime, so nothing the administrator configured
     * is touched. This is the path where the plugin really goes away.
     *
     * @param pluginId Plugin ID
     * @param purgeData Whether everything the plugin owns - its settings, its datasource
     *        configuration and data, and the files it stored - should be deleted along with it.
     *        The default an uninstall carries is false, so that a reinstall can pick up where
     *        the plugin left off and an administrator who wants it gone can still ask for the
     *        purge afterwards
     * @throws PluginPurgeFailedException when [purgeData] is set and the plugin could not delete
     *         its own data. The uninstall is refused rather than half-done: nothing has been
     *         removed at that point, so running it again once the cause is fixed is all it takes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginLifecycle
     */
    private fun removePlugin(pluginId: String, purgeData: Boolean) {
        val plugin = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId)) ?: return

        // A plugin that is not running still has to be able to delete its own data, and it
        // reaches that data - its tables, in a database the gateway never touches - through the
        // datasources only a mount builds. So a purge of a plugin that is down mounts it first,
        // for the exit hooks alone
        val runtime = pluginRuntimes.remove(pluginId)
            ?: if (purgeData) mountForPurge(plugin) else null
        val codes = apiInterfaceMapper.selectList(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
        ).mapNotNull { it.code }

        try {
            runtime?.let { unmount(it, purgeRequested = purgeData, uninstalling = true) }
        } catch (e: PluginPurgeFailedException) {
            // Refused, and refused at the one point where nothing has been removed yet: the row
            // this purge would have deleted is still there, its settings and its files are still
            // there, and running the uninstall again is all it takes once the cause is fixed.
            // The runtime is gone either way, so the reason goes where the plugin list can read
            // it
            logger.error("Uninstall of plugin '{}' refused: {}", pluginId, e.message)
            setLoadError(pluginId, e.message ?: "Purge failed")
            refreshCache()

            throw e
        }

        updateOrThrowException { removeById(plugin.id) }
        apiInterfaceMapper.delete(KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId))
        cleanupPermissionTree(pluginId, codes)
        plugin.fileHash?.let { runCatching { storageBlobService.removeFile(it) } }
        refreshCache()

        // Purged after the plugin's own teardown hooks, which unmount ran: onUninstall while
        // the class loader was still open, and onPurge while the datasources still were. A
        // failure here is logged rather than raised: the database rows are already gone, so
        // aborting would only leave a half-uninstalled plugin behind. Each kind is attempted
        // on its own, so one failure does not spare the others.
        if (purgeData) {
            runCatching { apiPluginSettingService.deleteByPlugin(pluginId) }.onFailure {
                logger.warn("Failed to purge the configuration of plugin '{}': {}", pluginId, it.message)
            }
            // The configuration of a datasource is ordinary settings, purged just above with
            // the rest; what is left here is the SQLite databases the gateway itself created
            runCatching { pluginDatasourceFactory.removeData(pluginId) }.onFailure {
                logger.warn("Failed to purge the datasource files of plugin '{}': {}", pluginId, it.message)
            }
            runCatching {
                fileStorageProvider.deleteAtPrefix(StorageKeyUtil.pluginBaseKey(pluginId))
            }.onSuccess {
                logger.info("Purged {} file(s) of plugin '{}'", it, pluginId)
            }.onFailure {
                logger.warn("Failed to purge the storage of plugin '{}': {}", pluginId, it.message)
            }
        }
    }

    /**
     * Mount a plugin that is not running, so that its own teardown hooks have something to run on
     *
     * Only a purge asks for this. `onPurge` is where a plugin drops the tables it owns, and it
     * reaches them through its datasources - which exist only while it is mounted - so a plugin
     * the administrator switched off has to be brought up once more in order to be taken apart.
     *
     * The mount is never registered, never routed and never visible to the request path: it
     * exists only to hand [unmount] a runtime. The plugin's `onStart` is deliberately not run
     * with it, since nothing here is starting the plugin.
     *
     * @param plugin Installed plugin row
     * @return Runtime for [unmount] to take down
     * @throws PluginPurgeFailedException when the plugin cannot be mounted, and so cannot be
     *         purged
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see removePlugin
     */
    private fun mountForPurge(plugin: ApiPlugin): PluginRuntime {
        val pluginId = plugin.pluginId
            ?: throw PluginPurgeFailedException("Plugin row ${plugin.id} has no plugin ID to purge")

        return try {
            val fileHash = plugin.fileHash?.takeIf { it.isNotBlank() }
                ?: throw PluginInstallException("Plugin has no jar file hash and cannot be purged")
            val bytes = storageBlobService.loadFile(fileHash)
                ?: throw PluginInstallException("Plugin jar blob not found for hash $fileHash")
            val pluginDir = Path.of(serverProperties.storage.pluginDir)
            Files.createDirectories(pluginDir)
            val jarPath = pluginDir.resolve("$fileHash.jar")
            Files.write(jarPath, bytes)

            prepareMount(
                jarPath = jarPath,
                jarName = plugin.jarName ?: "$pluginId.jar",
                persistBlob = false,
                checkVersion = false,
                expectedPluginId = pluginId,
                runStart = false
            ).toRuntime()
        } catch (e: Exception) {
            throw PluginPurgeFailedException(
                "Plugin '$pluginId' could not be mounted to delete its data: " +
                        (e.message ?: e::class.simpleName), e
            )
        }
    }

    /**
     * Re-mount every uploaded plugin from the blob store at startup. A failing
     * plugin is skipped and its error recorded; it never blocks gateway startup.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun loadUploadedPlugins() {
        val plugins = list(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::source, "UPLOADED"))
        plugins.forEach { plugin ->
            val pluginId = plugin.pluginId ?: return@forEach
            // A plugin the administrator switched off is one they asked not to run: it keeps
            // its registration, its configuration and its data, and stays out of the gateway
            // until it is switched back on
            if (plugin.enable != 1) {
                logger.info("Plugin '{}' is disabled; not loading it", pluginId)
                return@forEach
            }
            try {
                val fileHash = plugin.fileHash ?: throw PluginInstallException("Plugin has no jar file hash")
                val bytes = storageBlobService.loadFile(fileHash)
                    ?: throw PluginInstallException("Plugin jar blob not found")
                val pluginDir = Path.of(serverProperties.storage.pluginDir)
                Files.createDirectories(pluginDir)
                val jarPath = pluginDir.resolve("$fileHash.jar")
                Files.write(jarPath, bytes)
                // A plugin that fails to come back up keeps its registration: the row is
                // what records the failure, and deleting it would drop the plugin
                // silently instead of leaving an error for the administrator to read
                doInstall(
                    jarPath = jarPath, jarName = plugin.jarName ?: "$pluginId.jar",
                    persistBlob = false, checkVersion = false, preserveRegistration = true
                )
            } catch (e: Exception) {
                logger.error("Failed to load plugin '{}': {}", pluginId, e.message)
                setLoadError(pluginId, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Mount a plugin
     *
     * @param jarPath Local path of the signed plugin jar
     * @param jarName Original jar name
     * @param persistBlob Whether the jar should be written to the blob store
     * @param checkVersion Whether this is an install: the declared version is held against the
     *        installed one, and a plugin already there has its runtime replaced
     * @param preserveRegistration Whether the plugin's existing registration must
     *        survive a failure. The mounts that bring a plugin back from its own stored jar
     *        pass true: their rows existed before this call and are still the only record of
     *        them, so the rollback that protects a fresh install would instead destroy a
     *        healthy plugin
     * @return Installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see prepareMount
     * @see activateMount
     */
    private fun doInstall(
        jarPath: Path,
        jarName: String,
        persistBlob: Boolean,
        checkVersion: Boolean,
        preserveRegistration: Boolean = false
    ): ApiPluginVo =
        activateMount(
            prepareMount(jarPath, jarName, persistBlob, checkVersion, expectedPluginId = null),
            preserveRegistration
        )

    /**
     * Everything a mount builds before it is allowed to touch the gateway
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see prepareMount
     * @see activateMount
     * @see PluginRuntime
     */
    private class PreparedMount(
        val descriptor: PluginDescriptor,
        val fileHash: String,
        val signerKeyId: String,
        val jarName: String,
        val jarPath: Path,
        val openapiJson: String?,
        val configSchemaJson: String?,
        val classLoader: URLClassLoader,
        val libraries: PluginLibrarySet,
        val context: GenericApplicationContext,
        val endpoints: List<MountedEndpoint>,
        val datasources: Map<String, DataSource>,
        val pluginContext: PluginContext,
        val lifecycle: PluginLifecycle?,
        val savedFileHash: String?,

        /**
         * Version code the plugin was installed at before this mount, or null when it was not
         * installed at all. Non-null means activation is replacing a runtime rather than adding
         * one, and has to leave the rows that runtime's registration lives in alone
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val previousVersionCode: Int?,

        /**
         * Blob the plugin row pointed at before this mount, under the same gate as
         * [previousVersionCode]. Released once the row points at this mount's jar instead
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val previousFileHash: String?,

        /**
         * Enable flag the plugin had before this mount, or null when it was not installed.
         * Carried under the same gate as [previousVersionCode], and read only to keep a
         * replacement from starting a plugin the administrator had switched off
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val previousEnable: Int?,

        /**
         * Why the plugin's own `onStart` failed, or null when it came up
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see PluginLifecycle.onStart
         */
        val startupError: Throwable?
    ) {
        /**
         * The runtime record this mount becomes once it is activated
         *
         * @return Runtime holding what teardown reads
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see PluginRuntime
         */
        fun toRuntime() = PluginRuntime(
            pluginId = descriptor.pluginId,
            classLoader = classLoader,
            libraries = libraries,
            context = context,
            pluginContext = pluginContext,
            endpoints = endpoints,
            tempJarPath = jarPath,
            datasources = datasources,
            lifecycle = lifecycle
        )
    }

    /**
     * Build a plugin's mount without activating it
     *
     * It changes nothing the gateway serves: no route registered, no row written, no cache
     * refreshed.
     *
     * @param jarPath Local path of the signed plugin jar
     * @param jarName Original jar name
     * @param persistBlob Whether the jar should be written to the blob store
     * @param checkVersion Whether the declared version is held against the installed one - which
     *        refuses only a downgrade - and the plugin already being there is read as this mount
     *        replacing its runtime
     * @param expectedPluginId Plugin ID the jar must declare, or null to take the jar's own.
     *        A remount passes the install it is replacing, so a jar that turns out to belong to
     *        a different plugin is caught while the running one is still serving
     * @param runStart Whether the plugin's own `onStart` hook is run as part of the mount. Only
     *        a mount built to be purged passes false: that one exists to run the plugin's
     *        teardown hooks against the datasources it declares, and starting it on the way to
     *        taking it apart would be work nobody asked for
     * @return Built mount, ready to activate
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see activateMount
     */
    private fun prepareMount(
        jarPath: Path,
        jarName: String,
        persistBlob: Boolean,
        checkVersion: Boolean,
        expectedPluginId: String?,
        runStart: Boolean = true
    ): PreparedMount {
        var mountedLoader: URLClassLoader? = null
        var mountedLibraries: PluginLibrarySet? = null
        var mountedContext: GenericApplicationContext? = null
        var mountedDatasources = emptyMap<String, DataSource>()
        var savedFileHash: String? = null
        var publishedSchemaFor: String? = null
        try {
            // ---- 1. signature + trust ----
            val publicKeyPem = PluginSigner.readPublicKey(jarPath)
                ?: throw PluginSignatureInvalidException()
            val signerKeyId = try {
                PluginSigner.spkiFingerprint(publicKeyPem)
            } catch (_: Exception) {
                throw PluginSignatureInvalidException()
            }
            if (!PluginSigner.verify(jarPath, publicKeyPem)) {
                throw PluginSignatureInvalidException()
            }
            val trustKey = apiPluginTrustKeyService.getByKeyId(signerKeyId)
            if (trustKey == null || trustKey.enable != 1) {
                throw PluginNotTrustedException()
            }

            // ---- 2. descriptor + controller scan ----
            val descriptor = readDescriptor(jarPath)
            require(PluginDescriptor.PLUGIN_ID_REGEX.matches(descriptor.pluginId)) {
                "Invalid plugin ID: '${descriptor.pluginId}' (must match ${PluginDescriptor.PLUGIN_ID_REGEX})"
            }
            if (expectedPluginId != null && descriptor.pluginId != expectedPluginId) {
                throw PluginInstallException(
                    "Plugin jar blob belongs to '${descriptor.pluginId}', not '$expectedPluginId'"
                )
            }
            // Dependencies are materialized only after the signature and trust checks above
            val installedLibraries = pluginLibraryCache.install(jarPath)
            val libraries = pluginLibraryRegistry.acquire(installedLibraries)
            mountedLibraries = libraries
            val parentFirstPrefixes = if (PluginLibraryIndex.bundlesBouncyCastle(installedLibraries.index)) {
                logger.warn(
                    "Plugin '{}' bundles BouncyCastle; use Provider instances instead of Security.addProvider",
                    descriptor.pluginId
                )
                PluginClasspath.PARENT_FIRST_PREFIXES
            } else {
                PluginClasspath.PARENT_FIRST_PREFIXES + PluginClasspath.BOUNCY_CASTLE_PREFIX
            }
            val loader = PluginClassLoaderManager.create(
                jarPath.toUri().toURL(),
                libraries.loaders,
                parentFirstPrefixes
            )
            mountedLoader = loader
            val scan = scanJar(loader, jarPath)
            if (scan.controllers.isEmpty()) {
                throw PluginInstallException(
                    "No @ApiController class found in jar" + unloadableNote(scan.failures)
                )
            }
            // A class of the plugin's own that failed to load is refused here; one that belongs
            // to a bundled library is only worth a warning - see verifyLoadFailures
            verifyLoadFailures(scan, jarPath)
            // A plugin component outside the scanned roots is left unregistered, which is
            // invisible to the author until an injection fails with nothing pointing at the cause
            warnUnscannedComponents(scan, jarPath)

            // ---- 3. version rule ----
            val existing = getByPluginIdOrQuery(descriptor.pluginId)
            // What was installed before this mount, or null when there was nothing - and always
            // null off the install path, so that only an install replaces a runtime here: a
            // remount, a reload and a startup re-hydration have already done that themselves,
            // and would otherwise tear the same plugin down twice
            val previousVersionCode = if (checkVersion) existing?.versionCode else null
            // Carried for the same reason and under the same gate as the version code: the blob
            // the plugin row is about to stop pointing at has to be released once it does
            val previousFileHash = if (checkVersion) existing?.fileHash else null
            // Whether the administrator had this plugin switched off, which decides whether
            // replacing it may start it - a new version is not a reason to overrule them
            val previousEnable = if (checkVersion) existing?.enable else null
            // A downgrade is the one version change refused. Re-installing a version that is
            // already there is not one: it is the same plugin's code being replaced with a
            // build of it, which is what a development loop does over and over
            if (previousVersionCode != null && descriptor.versionCode < previousVersionCode) {
                throw PluginVersionConflictException(
                    "Version code ${descriptor.versionCode} is older than the installed $previousVersionCode"
                )
            }

            // ---- 4. persist jar into the blob store ----
            val fileHash = Files.readAllBytes(jarPath).sha256HexString()
            if (persistBlob) {
                savedFileHash = storageBlobService.saveFile(Files.readAllBytes(jarPath))
            }
            val configSchemaJson = readJarEntry(jarPath, CONFIG_ENTRY)
            val configSchema = parseConfigSchema(configSchemaJson)

            // Published before anything is built out of the plugin's settings, not merely before
            // the hook runs: the datasource pools below are built from those settings, and
            // whether a stored value is a secret - and so has to be decrypted - is known only
            // from the schema. A gateway start reaches the pools while the cache is still empty,
            // since refreshCache only runs once every plugin has been re-hydrated, which would
            // hand a pool its password as ciphertext. The row carrying the schema is written
            // only at activation, so this is also what lets a hook reading a setting find the
            // declared default. Taken back out if the build fails - see below
            pluginConfigSchemaCache.put(descriptor.pluginId, configSchema)
            publishedSchemaFor = descriptor.pluginId

            // ---- 5. child context + beans ----
            // Declaring a datasource is what earns a plugin one: without the declaration
            // there is nothing for the administrator to configure and nothing for the
            // gateway to inject, whatever rows happen to be left over. A declared one that
            // is not configured is simply absent - see PluginDatasourceFactory.create
            val datasources = pluginDatasourceFactory.create(descriptor.pluginId, configSchema)
            mountedDatasources = datasources
            val pluginStorage = pluginStorageFactory.create(descriptor.pluginId)
            // Scanned, instantiated and refreshed under the plugin's own loader: everything in
            // there is the plugin's code, and a library it bundles has to be able to find itself
            // when it asks the thread for a class loader
            val childContext = PluginClassLoaderManager.withPluginClassLoader(loader) {
                createChildContext(
                    jarPath, loader, scan.controllers, descriptor.pluginId, datasources, pluginStorage
                )
            }
            mountedContext = childContext
            val controllers = scan.controllers.map { childContext.getBean(it) }

            // ---- 6. endpoints, computed but not registered ----
            val endpoints = mountEndpoints(descriptor.pluginId, controllers)

            // ---- 7. plugin context + lifecycle ----
            val pluginContext = PluginContextImpl(
                pluginId = descriptor.pluginId,
                datasources = datasources,
                storage = pluginStorage,
                apiAccountService = apiAccountService,
                apiPluginSettingService = apiPluginSettingService,
                configSchemaCache = pluginConfigSchemaCache,
                pluginSettingReader = pluginSettingReader,
                interfaceLookup = { getByCode(it) }
            )
            val lifecycle = findLifecycle(childContext, descriptor)

            // The plugin's own startup is caught rather than allowed out: it is where a plugin
            // reaches its datasources, so it is the step an administrator's misconfigured
            // connection fails in - and refusing the install over that would leave them nothing
            // to fix it with, since the row and the config API come from the install itself.
            // A failure here still means the plugin does not serve: activation registers it
            // without routes and reports the reason in load_error.
            // The plugin's own startup is skipped for a mount that will not be activated either
            // way: the throwaway mount a purge builds, and a mount over a plugin the
            // administrator has switched off. That second one is the load-bearing case - an
            // install replaces what is stored for a disabled plugin, and it must not double as
            // a way to start one, which is exactly what running this hook would be
            val startupError = if (!runStart || previousEnable == 0) {
                null
            } else {
                runCatching {
                    lifecycle?.let {
                        PluginClassLoaderManager.withPluginClassLoader(loader) { it.onStart(pluginContext) }
                    }
                }.exceptionOrNull()
            }
            if (startupError != null) {
                logger.warn(
                    "Plugin '{}' was built but did not start: {}", descriptor.pluginId, startupError.message
                )
            }

            // Generated from the same scan the endpoints came from, rather than read out of the
            // jar: a fragment can no longer describe an interface this mount did not find. A
            // failure here costs the plugin its documentation and nothing else - it still serves,
            // and the reader already renders a fragment that is absent
            val openapiJson = runCatching { pluginOpenApiGenerator.generate(endpoints) }
                .onFailure {
                    logger.warn(
                        "Plugin '{}': OpenAPI fragment generation failed, docs will omit it: {}",
                        descriptor.pluginId, it.message
                    )
                }
                .getOrNull()

            return PreparedMount(
                descriptor = descriptor,
                fileHash = fileHash,
                signerKeyId = signerKeyId,
                jarName = jarName,
                jarPath = jarPath,
                openapiJson = openapiJson,
                configSchemaJson = configSchemaJson,
                classLoader = loader,
                libraries = libraries,
                context = childContext,
                endpoints = endpoints,
                datasources = datasources,
                pluginContext = pluginContext,
                lifecycle = lifecycle,
                savedFileHash = savedFileHash,
                previousVersionCode = previousVersionCode,
                previousFileHash = previousFileHash,
                previousEnable = previousEnable,
                startupError = startupError
            )
        } catch (e: Exception) {
            mountedContext?.let { runCatching { it.close() } }
            // Closed here as well as on unmount: a mount that failed after the pools were
            // built would otherwise leave a pool and its threads behind for good, since no
            // runtime is ever recorded for a mount that did not finish
            closeDatasources(mountedDatasources)
            mountedLoader?.let { runCatching { it.close() } }
            mountedLibraries?.let { runCatching { it.release() } }
            savedFileHash?.let { runCatching { storageBlobService.removeFile(it) } }
            runCatching { Files.deleteIfExists(jarPath) }
            // A schema published for a hook that never ran is put back to what the rows say:
            // they still describe whichever version is actually installed, and a mount that
            // failed must not leave the plugin reading its settings under a schema that never
            // took effect
            publishedSchemaFor?.let { pluginId ->
                runCatching {
                    pluginConfigSchemaCache.put(
                        pluginId,
                        parseConfigSchemaOrNull(getByPluginIdOrQuery(pluginId)?.configSchema)
                    )
                }
            }
            throw e
        }
    }

    /**
     * Activate a built mount
     *
     * A plugin whose own `onStart` failed is registered without being mounted: its rows, its jar
     * and its configuration are stored, no route is registered, no runtime is recorded, and the
     * reason goes into `load_error`.
     *
     * @param prepared Built mount
     * A mount that lands over a plugin already installed replaces its runtime without touching
     * its registration: the row, the interface rows and the permission nodes are matched rather
     * than rebuilt, which is what keeps the administrator's pricing, access settings and role
     * grants across a replacement. Only the runtime is taken down, and only the strict upgrade
     * among the replacements counts as taking that version out of the gateway.
     *
     * @param preserveRegistration Whether the plugin's existing registration must survive a
     *        failure even though [PreparedMount.previousVersionCode] is null - true for the
     *        mounts that bring a plugin back from its own stored jar, where the rows existed
     *        before this call and are still its only record, so the rollback that protects a
     *        fresh install would instead destroy a healthy plugin
     * @return Installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see prepareMount
     */
    private fun activateMount(prepared: PreparedMount, preserveRegistration: Boolean): ApiPluginVo {
        val pluginId = prepared.descriptor.pluginId
        // Rows that were already there are the only record of what the administrator configured
        // and granted: a mount over an existing plugin leaves them standing, both while it runs
        // and if it fails
        val rowsExist = preserveRegistration || prepared.previousVersionCode != null
        var activatedEndpoints = false
        var dbRowsCreated = false
        var mountedRuntime: PluginRuntime? = null
        try {
            // The old runtime is the only thing a replacement takes down. Its rows, its
            // interface rows and its permission nodes are deliberately left standing: the
            // upserts below match them on (path, method) and on code, and that match is what
            // carries the administrator's pricing, access settings and role grants across a
            // replacement instead of rebuilding them under fresh ids
            prepared.previousVersionCode?.let { previousCode ->
                pluginRuntimes.remove(pluginId)?.let { runtime ->
                    // A version that moved forward is an upgrade, and an upgrade takes the old
                    // version out of the gateway. Replacing the version that is already there
                    // is a reload: nothing is being taken out, and the plugin is coming
                    // straight back as the new build of the same version
                    unmount(
                        runtime,
                        deleteJar = false,
                        uninstalling = prepared.descriptor.versionCode > previousCode
                    )
                }
            }

            // Registered before the routes, and regardless of whether the plugin came up: the
            // row is what the config API resolves, so it has to exist for the administrator to
            // fix what stopped the plugin from starting
            upsertPluginRow(
                prepared.descriptor, prepared.fileHash, prepared.jarName, prepared.signerKeyId,
                prepared.openapiJson, prepared.configSchemaJson
            )
            dbRowsCreated = true
            // The row points at this mount's jar from here on, so the blob it pointed at before
            // is owed its release. Unconditional, identical bytes included: the save that
            // preceded this mount added the very reference this takes back. The materialized
            // file is a different matter - under identical bytes it is the file this mount just
            // read, and deleting it would break the plugin it just replaced
            prepared.previousFileHash?.let { previousHash ->
                runCatching { storageBlobService.removeFile(previousHash) }
                if (previousHash != prepared.fileHash) {
                    runCatching {
                        Files.deleteIfExists(
                            Path.of(serverProperties.storage.pluginDir).resolve("$previousHash.jar")
                        )
                    }
                }
            }
            val pluginMenu = ensureMenu(
                pluginId, prepared.descriptor.name, API_ROOT_MENU_ID, API_MODULE_ID
            )

            // What the plugin's endpoints were, read before the loop below writes over them. The
            // loop matches an interface on (path, method), so an endpoint whose route is gone - or
            // whose code changed - would otherwise leave its previous row behind for good, and
            // upsertOperation can never match a code the mount no longer derives
            val previousInterfaces = apiInterfaceMapper.selectList(
                KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
            )

            val scopeIds = ConcurrentHashMap<String, Long>()
            prepared.endpoints.forEach { endpoint ->
                val scopeId = scopeIds.getOrPut("v${endpoint.apiVersion}") {
                    ensureScope("v${endpoint.apiVersion}", pluginMenu.id!!).id!!
                }
                upsertInterface(
                    pluginId, endpoint.code, endpoint.name, endpoint.description,
                    endpoint.fullPath, endpoint.httpMethod, endpoint.apiVersion
                )
                upsertOperation(endpoint.code, endpoint.name, scopeId)
            }

            // Reconcile, after the endpoints are in place so a scope they still use is never
            // emptied and rebuilt: drop the interface rows whose route this mount no longer
            // serves, and the permission entries under a code it no longer derives. A route that
            // kept its path but changed its code keeps both - the row is updated in place and the
            // code is still current - so this removes what is gone, not what moved
            val currentRoutes = prepared.endpoints.mapTo(HashSet()) { it.fullPath to it.httpMethod }
            previousInterfaces
                .filterNot { (it.path to it.method) in currentRoutes }
                .forEach { apiInterfaceMapper.deleteById(it.id) }
            val currentCodes = prepared.endpoints.mapTo(HashSet()) { it.code }
            val orphanedCodes = previousInterfaces.mapNotNull { it.code }.filterNot { it in currentCodes }
            if (orphanedCodes.isNotEmpty()) {
                cleanupPermissionTree(pluginId, orphanedCodes)
            }

            if (prepared.startupError != null) {
                // Registered but not mounted. What was built is released here rather than by an
                // unmount, because nothing will ever unmount a plugin that has no runtime - but
                // the jar is kept in the blob store, which is what a later remount, once the
                // administrator has fixed what the plugin could not start against, reads
                closePrepared(prepared)
                runCatching { Files.deleteIfExists(prepared.jarPath) }
                setLoadError(pluginId, prepared.startupError.message ?: "Plugin did not start")
                refreshCache()

                return getByPluginIdOrQuery(pluginId)
                    ?.toVo()
                    ?: throw PluginInstallException("Failed to query the installed plugin")
            }

            if (prepared.previousEnable == 0) {
                // Registered but not brought up, because the administrator has this plugin
                // switched off: installing a version replaces what is stored for the plugin, it
                // does not overrule the decision not to run it. The rows are up to date with the
                // new build, no route is registered and no runtime is recorded - and unlike a
                // plugin that failed to start, nothing went wrong here, so load_error stays out
                // of it. Switching it back on mounts exactly this jar
                closePrepared(prepared)
                refreshCache()

                return getByPluginIdOrQuery(pluginId)
                    ?.toVo()
                    ?: throw PluginInstallException("Failed to query the installed plugin")
            }

            activateEndpoints(prepared.endpoints)
            activatedEndpoints = true

            val runtime = prepared.toRuntime()
            pluginRuntimes[pluginId] = runtime
            mountedRuntime = runtime
            clearLoadError(pluginId)
            // One refresh, once the rows and the error state are final: the cache is what a
            // list reads a plugin out of and what the request path admits one through, so it is
            // published complete rather than in the pieces it was built from
            refreshCache()

            return getByPluginIdOrQuery(pluginId)
                ?.toVo()
                ?: throw PluginInstallException("Failed to query the installed plugin")
        } catch (e: Exception) {
            if (activatedEndpoints) {
                prepared.endpoints.forEach {
                    runCatching { requestMappingHandlerMapping.unregisterMapping(it.info) }
                }
            }
            runCatching { prepared.context.close() }
            closeDatasources(prepared.datasources)
            runCatching { prepared.classLoader.close() }
            prepared.savedFileHash?.let { runCatching { storageBlobService.removeFile(it) } }
            runCatching { Files.deleteIfExists(prepared.jarPath) }
            // Exact removal: the runtime goes in only once the mount is complete, so a bare
            // remove(pluginId) could also drop a newer runtime that legitimately replaced this
            // one
            mountedRuntime?.let { pluginRuntimes.remove(pluginId, it) }
            // Roll back database rows created during the failed install so no orphaned
            // plugin / interface rows remain. A mount over an existing plugin must never run
            // this: the rows it would delete are the record of a plugin that existed before
            // the call, and deleting them would turn a failed configuration change or a
            // failed upgrade into a silent uninstall.
            if (dbRowsCreated && !rowsExist) {
                runCatching {
                    val created = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))
                    created?.let { updateOrThrowException { removeById(it.id) } }
                    val codes = apiInterfaceMapper.selectList(
                        KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
                    ).mapNotNull { it.code }
                    apiInterfaceMapper.delete(KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId))
                    cleanupPermissionTree(pluginId, codes)
                }
            }
            if (rowsExist) {
                // The rows survive a failed mount over an existing plugin, so from here on they
                // claim a plugin is installed while no route serves it. load_error is the only
                // channel the gateway has to publish that, and it reaches the administrator
                // through the plugin list
                setLoadError(pluginId, e.message ?: "Unknown error")
            }
            // Refreshed even when nothing was written: a rollback or a load error the cache has
            // not seen would leave the plugin list describing a state that no longer exists
            refreshCache()
            throw e
        }
    }

    /**
     * Tear the runtime of a plugin down
     *
     * Waits for the plugin's in-flight requests first, bounded by
     * [IN_FLIGHT_DRAIN_TIMEOUT_MILLIS]; a request that has not entered yet is refused rather than
     * waited for.
     *
     * @param runtime Runtime to tear down
     * @param deleteJar Whether the local jar file should be deleted with it. A remount passes
     *        false, and so does a replacement: the jar is content-addressed, and under
     *        identical bytes it is the very file the new mount reads from
     * @param purgeRequested Whether the plugin's own data is being deleted along with it, which
     *        is what decides whether its `onPurge` hook runs. An upgrade and a remount pass
     *        false, so a plugin is never asked to destroy data it is about to need again. A
     *        failure in the hook is not swallowed: it is raised once the teardown has finished,
     *        so a purge that did not happen cannot be mistaken for one that did
     * @param uninstalling Whether this plugin is going away for good, which is what decides
     *        whether its `onUninstall` hook runs. An upgrade passes true as well, since it takes
     *        the old version down on the way to installing the new one. Everything else passes
     *        false - a `reload`, a remount, replacing the same version and a shutdown all leave
     *        the plugin coming right back
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginInFlightTracker
     */
    private fun unmount(
        runtime: PluginRuntime,
        deleteJar: Boolean = true,
        purgeRequested: Boolean = false,
        uninstalling: Boolean = false
    ) {
        val pluginId = runtime.pluginId

        // Refusing new requests comes before the routes go, so a request that resolved its
        // handler a moment ago is turned away cleanly instead of being counted and waited for
        pluginInFlightTracker.beginDrain(pluginId)
        runtime.endpoints.forEach { runCatching { requestMappingHandlerMapping.unregisterMapping(it.info) } }

        if (!pluginInFlightTracker.awaitDrain(pluginId, IN_FLIGHT_DRAIN_TIMEOUT_MILLIS)) {
            logger.warn(
                "Draining plugin '{}' timed out after {} ms; tearing it down with requests still in flight",
                pluginId, IN_FLIGHT_DRAIN_TIMEOUT_MILLIS
            )
        }

        // Purged while everything the plugin holds is still open and nothing is running: an
        // external database cannot be reached once the pools are closed, and a table dropped
        // under a request still using it would fail that request rather than the purge.
        //
        // A failure is held rather than thrown from here, and re-raised at the end of the
        // method: the teardown below is what releases the class loader and its pools, so
        // abandoning it would leak both - and the caller still has to be told, because a purge
        // that did not happen must not be mistaken for one that did
        var purgeFailure: Throwable? = null
        if (purgeRequested) {
            runtime.lifecycle?.let {
                runCatching {
                    PluginClassLoaderManager.withPluginClassLoader(runtime.classLoader) {
                        it.onPurge(runtime.pluginContext)
                    }
                }.onFailure {
                    // Logged with the exception, not just its message: the stack is the only
                    // record of how far into a plugin's own data cleanup it got
                    logger.error("Plugin '{}' onPurge failed", pluginId, it)
                    purgeFailure = it
                }
            }
        }
        runtime.lifecycle?.let {
            runCatching {
                PluginClassLoaderManager.withPluginClassLoader(runtime.classLoader) {
                    it.onStop(runtime.pluginContext)
                }
            }.onFailure {
                logger.warn("Plugin '{}' onStop failed: {}", pluginId, it.message)
            }
        }
        runCatching { runtime.context.close() }
        // Closed after the context, so a lifecycle bean shutting down can still use its
        // connection, and before the class loader, because Hikari's housekeeper and
        // connection-adder threads reach into the driver classes that loader owns. Hikari's
        // close is idempotent, so a second close from another path stays harmless.
        closeDatasources(runtime.datasources)
        // The last hook, and the reason it runs here rather than after this method returns:
        // it is plugin code, and a class it has not loaded yet still has to resolve out of the
        // jar, which a closed loader no longer does. Its datasources are closed by now, which
        // is deliberate - an upgrade runs this hook too, and a hook that could still reach them
        // would be a place to destroy data the upgrade needs back
        if (uninstalling) {
            runtime.lifecycle?.let {
                runCatching {
                    PluginClassLoaderManager.withPluginClassLoader(runtime.classLoader) {
                        it.onUninstall(runtime.pluginContext)
                    }
                }.onFailure {
                    logger.warn("Plugin '{}' onUninstall failed: {}", pluginId, it.message)
                }
            }
        }
        runCatching { runtime.classLoader.close() }
        runtime.libraries.release()
        // Forgotten after the resources, so a straggler that outlived the drain decrements the
        // state nothing waits on any more rather than the next mount's
        pluginInFlightTracker.release(pluginId)
        if (deleteJar) {
            runCatching { Files.deleteIfExists(runtime.tempJarPath) }
        }

        // Raised here, with the plugin's resources already released: the caller is a purge that
        // has to be refused, and it cannot be refused by leaving a class loader behind
        purgeFailure?.let {
            throw PluginPurgeFailedException(
                "Plugin '$pluginId' could not delete its data: ${it.message ?: it::class.simpleName}", it
            )
        }
    }

    /**
     * Release everything a built mount holds
     *
     * @param prepared Built mount
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see closeDatasources
     */
    private fun closePrepared(prepared: PreparedMount) {
        runCatching { prepared.context.close() }
        closeDatasources(prepared.datasources)
        runCatching { prepared.classLoader.close() }
        runCatching { prepared.libraries.release() }
    }

    /**
     * Close every pool of a mount
     *
     * @param datasources Datasources to close
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun closeDatasources(datasources: Map<String, DataSource>) {
        datasources.values.forEach { runCatching { (it as? AutoCloseable)?.close() } }
    }

    private fun createChildContext(
        jarPath: Path,
        loader: URLClassLoader,
        controllerClasses: List<Class<*>>,
        pluginId: String,
        datasources: Map<String, DataSource>,
        pluginStorage: PluginStorage
    ): GenericApplicationContext {
        val ctx = GenericApplicationContext()
        ctx.parent = applicationContext
        ctx.classLoader = loader
        ctx.id = "plugin-$pluginId"

        val environment = StandardEnvironment()
        (applicationContext.environment as? ConfigurableEnvironment)?.propertySources
            ?.forEach { environment.propertySources.addLast(it) }
        ctx.environment = environment

        val scanner = ClassPathBeanDefinitionScanner(ctx, false)
        scanner.setResourceLoader(PathMatchingResourcePatternResolver(loader))
        scanner.addIncludeFilter(AnnotationTypeFilter(Component::class.java))
        // Controllers come out of this scan too, and need their own filter to: @ApiController is
        // deliberately not a Spring stereotype - it is not a @RestController, so it is not a
        // @Component - which is fine only as long as the scanner is told explicitly that a class
        // carrying it is a bean. Without this filter no controller would match, and the getBean
        // call that pulls each one out of the context would find nothing.
        scanner.addIncludeFilter(AnnotationTypeFilter(ApiController::class.java))
        // Scan the root packages that lead to the plugin's controllers so collaborators
        // (e.g. @Service beans) living anywhere under those roots are picked up, while
        // a fat jar's bundled-library components are left alone.
        val roots = PluginClassLoaderManager.controllerRootPackages(jarPath, controllerClasses)
        if (roots.isEmpty()) {
            throw PluginInstallException("Cannot derive scan packages for plugin $pluginId")
        }
        // The scanner parses every candidate class's metadata, and Spring validates @AliasFor
        // declarations while it does: a mapping that declares `value` and `path` differently -
        // which Spring refuses to mirror - fails right here, before any route exists. Caught so
        // the cause reaches the administrator instead of surfacing as a bare SYSTEM_ERROR, and so
        // any other unreadable metadata names the jar it came from.
        try {
            scanner.scan(*roots.toTypedArray())
        } catch (e: Exception) {
            // The outermost message only names the class being read; the reason is at the bottom
            // of the chain (e.g. "attribute 'path' and its alias 'value' have different values"),
            // and that is the part an author can act on
            val reason = generateSequence<Throwable>(e) { it.cause }.last().message ?: e.toString()
            throw PluginInstallException(
                "Cannot read the annotations of plugin jar '${jarPath.fileName}': $reason"
            )
        }

        ctx.registerBean("pluginContext", PluginContext::class.java, Supplier {
            PluginContextImpl(
                pluginId = pluginId,
                datasources = datasources,
                storage = pluginStorage,
                apiAccountService = apiAccountService,
                apiPluginSettingService = apiPluginSettingService,
                configSchemaCache = pluginConfigSchemaCache,
                pluginSettingReader = pluginSettingReader,
                interfaceLookup = { getByCode(it) }
            )
        })
        // One bean per datasource, named after the plugin's own name for it, so a component
        // that wants one injected can ask by name instead of going through the context.
        // Registered with an explicit close so a child context closed on its own still
        // releases the pool. The name is prefixed because a plugin's own components are
        // registered under their class names, and a datasource named after one would clash.
        datasources.forEach { (name, datasource) ->
            ctx.registerBean(
                "pluginDataSource.$name",
                DataSource::class.java,
                Supplier { datasource },
                BeanDefinitionCustomizer { it.setDestroyMethodName("close") }
            )
        }

        ctx.refresh()
        return ctx
    }

    /**
     * Compute the endpoints of a plugin's controllers
     *
     * @param pluginId Plugin ID from the descriptor, which namespaces every path and every code
     * @param controllers Controller bean instances
     * @return Mounted endpoints, not yet registered
     * @throws PluginInstallException when a controller declares an API version below 1 or more than
     *         one base path, a mapped method declares more than one path, a mapped method declares
     *         no usable operation ID, its code exceeds [MAX_CODE_LENGTH], two endpoints under one
     *         version resolve to the same code, or two of them claim the same path and HTTP method
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see activateEndpoints
     */
    private fun mountEndpoints(pluginId: String, controllers: List<Any>): List<MountedEndpoint> {
        val endpoints = mutableListOf<MountedEndpoint>()
        val codeOwners = mutableMapOf<String, Method>()
        val pathOwners = mutableMapOf<String, Method>()
        controllers.forEach { controller ->
            val targetClass = AopUtils.getTargetClass(controller)
            val annotation = targetClass.getAnnotation(ApiController::class.java)
                ?: throw PluginInstallException("${targetClass.name} is not annotated with @ApiController")
            // A version below 1 composes a path the router's /v(\d+) never matches: the mount
            // would succeed, the endpoint would be written to the database, and every request to
            // it would 404 with nothing pointing at why. versionCode is checked the same way.
            if (annotation.version < 1) {
                throw PluginInstallException(
                    "${targetClass.name} declares @ApiController(version = ${annotation.version}); " +
                            "the API version must be at least 1"
                )
            }
            // The base path is an array too, and only one is supported: a second entry would be
            // dropped, leaving endpoints that look reachable under both but are mounted under one
            if (annotation.path.size > 1) {
                throw PluginInstallException(
                    "${targetClass.name} declares @ApiController(path = [...]) with " +
                            "${annotation.path.size} paths; the plugin mount supports exactly one " +
                            "base path per controller"
                )
            }
            val basePath = annotation.path.firstOrNull()?.trim('/') ?: ""
            targetClass.declaredMethods.forEach { method ->
                val mapping = resolveMapping(method) ?: return@forEach
                val operation = method.getAnnotation(SwaggerOperation::class.java)
                // Every mapped method has to declare its identity, and the mount is refused -
                // naming the method - rather than falling back to the method name: the code this
                // produces is what the interface row and its permission entry are keyed by, so a
                // silent fallback would persist an identity the author never chose and then leave
                // the operationId-derived row behind as an orphan on the next mount
                if (operation == null || operation.operationId.isBlank()) {
                    throw PluginInstallException(
                        "Endpoint ${targetClass.name}#${method.name} must declare a non-blank " +
                                "@Operation(operationId = \"...\"): the operation ID is the " +
                                "endpoint's persisted identity"
                    )
                }
                val operationId = operation.operationId
                val code = buildCode(pluginId, annotation.version, method)
                if (code.length > MAX_CODE_LENGTH) {
                    throw PluginInstallException(
                        "Endpoint ${targetClass.name}#${method.name} produces a ${code.length}-" +
                                "character API code, over the $MAX_CODE_LENGTH-character limit: " +
                                "shorten its operation ID '$operationId'"
                    )
                }
                // Unique per version, because the code embeds the version: the same operation ID
                // may be declared once under each @ApiController version, but declaring it twice
                // under one version would make two endpoints claim a single identity
                codeOwners.put(code, method)?.let { previous ->
                    throw PluginInstallException(
                        "Duplicate operation ID: ${previous.declaringClass.name}#${previous.name} " +
                                "and ${method.declaringClass.name}#${method.name} both map to the " +
                                "code '$code'; an operation ID must be unique within a plugin version"
                    )
                }
                val methodPath = mapping.path.trim('/')
                val fullPath = buildString {
                    append("/api/").append(pluginId).append("/v").append(annotation.version)
                    if (basePath.isNotEmpty()) append("/").append(basePath)
                    if (methodPath.isNotEmpty()) append("/").append(methodPath)
                }
                // Two endpoints of one plugin claiming the same path and method would register as
                // an ambiguous mapping and blow up inside registerMapping with a raw Spring
                // exception - after the rows were written. Refused here instead, naming both
                // methods, so a bad plugin still leaves nothing behind
                val pathKey = "${mapping.method.name} $fullPath"
                pathOwners.put(pathKey, method)?.let { previous ->
                    throw PluginInstallException(
                        "${previous.declaringClass.name}#${previous.name} and " +
                                "${method.declaringClass.name}#${method.name} both map to " +
                                "$pathKey; a path and HTTP method must be unique within a plugin version"
                    )
                }
                val templatePath = buildString {
                    append("/api/{PLUGIN}/v{API_VERSION}")
                    if (basePath.isNotEmpty()) append("/").append(basePath)
                    if (methodPath.isNotEmpty()) append("/").append(methodPath)
                }
                val info = RequestMappingInfo
                    .paths(templatePath)
                    .methods(mapping.method)
                    .params(*mapping.params.toTypedArray())
                    .headers(*mapping.headers.toTypedArray())
                    .produces(*mapping.produces.toTypedArray())
                    .consumes(*mapping.consumes.toTypedArray())
                    .customCondition(ApiVersionCondition(pluginId, annotation.version))
                    .build()

                endpoints += MountedEndpoint(
                    controller = controller,
                    method = method,
                    code = code,
                    name = operation.summary.takeIf { it.isNotBlank() } ?: method.name,
                    description = operation.description.takeIf { it.isNotBlank() } ?: "",
                    fullPath = fullPath,
                    httpMethod = mapping.method.name,
                    apiVersion = annotation.version,
                    info = info
                )
            }
        }
        return endpoints
    }

    /**
     * Register a plugin's computed endpoints with the dispatcher
     *
     * @param endpoints Endpoints computed by [mountEndpoints]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see mountEndpoints
     */
    private fun activateEndpoints(endpoints: List<MountedEndpoint>) {
        endpoints.forEach {
            requestMappingHandlerMapping.registerMapping(it.info, it.controller, it.method)
        }
    }

    private data class EndpointMapping(
        val path: String,
        val method: RequestMethod,
        val params: List<String>,
        val headers: List<String>,
        val produces: List<String>,
        val consumes: List<String>
    )

    /**
     * The single path a mapping annotation declares
     *
     * `value` is read as a fallback because it is `path`'s alias and Kotlin puts a positional
     * argument - `@GetMapping("/a")` - into `value`.
     *
     * @param method Endpoint method, named when the declaration is refused
     * @param path `path` attribute as declared
     * @param value `value` attribute as declared
     * @return The declared path, or "" when the method declares none
     * @throws PluginInstallException when more than one path is declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun singlePath(method: Method, path: Array<String>, value: Array<String>): String {
        val paths = path.ifEmpty { value }
        if (paths.size > 1) {
            throw PluginInstallException(
                "Endpoint ${method.declaringClass.name}#${method.name} declares ${paths.size} paths " +
                        "(${paths.joinToString(", ")}); the plugin mount maps exactly one path per " +
                        "method - declare a method for each path, or map a common prefix"
            )
        }
        return paths.firstOrNull() ?: ""
    }

    private fun resolveMapping(method: Method): EndpointMapping? {
        val get = method.getAnnotation(GetMapping::class.java)
        val post = method.getAnnotation(PostMapping::class.java)
        val put = method.getAnnotation(PutMapping::class.java)
        val patch = method.getAnnotation(PatchMapping::class.java)
        val delete = method.getAnnotation(DeleteMapping::class.java)

        return when {
            get != null -> EndpointMapping(
                singlePath(method, get.path, get.value),
                RequestMethod.GET, get.params.toList(), get.headers.toList(),
                get.produces.toList(), get.consumes.toList()
            )

            post != null -> EndpointMapping(
                singlePath(method, post.path, post.value),
                RequestMethod.POST, post.params.toList(), post.headers.toList(),
                post.produces.toList(), post.consumes.toList()
            )

            put != null -> EndpointMapping(
                singlePath(method, put.path, put.value),
                RequestMethod.PUT, put.params.toList(), put.headers.toList(),
                put.produces.toList(), put.consumes.toList()
            )

            patch != null -> EndpointMapping(
                singlePath(method, patch.path, patch.value),
                RequestMethod.PATCH, patch.params.toList(), patch.headers.toList(),
                patch.produces.toList(), patch.consumes.toList()
            )

            delete != null -> EndpointMapping(
                singlePath(method, delete.path, delete.value),
                RequestMethod.DELETE, delete.params.toList(), delete.headers.toList(),
                delete.produces.toList(), delete.consumes.toList()
            )

            // A method mapped any other way - a bare @RequestMapping, or a composed annotation the
            // plugin author wrote - is refused rather than skipped: silently mounting fewer
            // endpoints than the jar declares is exactly what nothing here should do quietly.
            // hasAnnotation follows meta-annotations, so a composed mapping is caught as well as a
            // direct @RequestMapping. The five shortcuts above have already returned, so this only
            // sees methods that carry no supported mapping of their own.
            AnnotatedElementUtils.hasAnnotation(method, RequestMapping::class.java) ->
                throw PluginInstallException(
                    "Endpoint ${method.declaringClass.name}#${method.name} is mapped with " +
                            "@RequestMapping, which the plugin mount does not support: use " +
                            "@GetMapping, @PostMapping, @PutMapping, @PatchMapping or @DeleteMapping"
                )

            else -> null
        }
    }

    /**
     * What one pass over a plugin jar found
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private data class JarScan(
        /**
         * Classes carrying [ApiController]
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val controllers: List<Class<*>>,

        /**
         * Classes carrying a Spring stereotype - `@Component` and the annotations meta-annotated
         * with it, such as `@Service`
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val components: List<Class<*>>,

        /**
         * Binary names of classes that could not be loaded
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val failures: List<String>
    )

    /**
     * Walk a plugin jar once and load every class in it
     *
     * @param loader Plugin class loader
     * @param jarPath Plugin jar path
     * @return Scan result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun scanJar(loader: URLClassLoader, jarPath: Path): JarScan {
        val controllers = mutableListOf<Class<*>>()
        val components = mutableListOf<Class<*>>()
        val failures = mutableListOf<String>()
        JarFile(jarPath.toFile()).use { jarFile ->
            jarFile.entries().asSequence().forEach { entry ->
                if (entry.isDirectory || !entry.name.endsWith(".class")) return@forEach
                val className = entry.name.removeSuffix(".class").replace('/', '.')
                if (className.contains('$')) return@forEach
                try {
                    val clazz = loader.loadClass(className)
                    if (clazz.getAnnotation(ApiController::class.java) != null) {
                        controllers.add(clazz)
                    }
                    // hasAnnotation rather than isAnnotationPresent: the latter does not follow
                    // meta-annotations, so it would miss every @Service
                    if (AnnotatedElementUtils.hasAnnotation(clazz, Component::class.java)) {
                        components.add(clazz)
                    }
                } catch (_: Throwable) {
                    // Recorded rather than logged and dropped: a class that cannot be loaded is
                    // usually an optional helper the plugin bundled, but one under a controller
                    // root package is the plugin's own code, and dropping that silently would make
                    // the plugin come up with fewer endpoints than it declares. Which of the two
                    // it is - and so whether the install is refused - is decided by
                    // verifyLoadFailures once the controllers are known
                    failures.add(className)
                }
            }
        }
        return JarScan(controllers, components, failures)
    }

    /**
     * Refuse the install when a class the plugin owns failed to load
     *
     * @param scan Scan result
     * @param jarPath Plugin jar path
     * @throws PluginInstallException when a class under a controller root package did not load
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see scanJar
     * @see controllerRootPackages
     */
    private fun verifyLoadFailures(scan: JarScan, jarPath: Path) {
        if (scan.failures.isEmpty()) return
        val roots = PluginClassLoaderManager.controllerRootPackages(jarPath, scan.controllers)
        val owned = scan.failures.filter { isUnderAnyPackage(it, roots) }
        if (owned.isNotEmpty()) {
            throw PluginInstallException(
                "Class(es) of plugin jar '${jarPath.fileName}' under its controller root packages " +
                        "could not be loaded: ${owned.joinToString(", ")}. A plugin class that fails " +
                        "to load would leave the mount short of the endpoints it declares"
            )
        }
        logger.warn(
            "Skipping {} class(es) of plugin jar '{}' that could not be loaded: {}",
            scan.failures.size, jarPath.fileName, scan.failures.joinToString(", ")
        )
    }

    /**
     * Warn about plugin components that fall outside the scanned roots
     *
     * @param scan Scan result
     * @param jarPath Plugin jar path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see controllerRootPackages
     */
    private fun warnUnscannedComponents(scan: JarScan, jarPath: Path) {
        if (scan.components.isEmpty()) return
        val roots = PluginClassLoaderManager.controllerRootPackages(jarPath, scan.controllers)
        scan.components
            .filterNot { isUnderAnyPackage(it.name, roots) }
            .forEach {
                logger.warn(
                    "Component '{}' of plugin jar '{}' is outside the scanned root packages {} and " +
                            "will not be registered as a bean; move it under one of them to have it scanned",
                    it.name, jarPath.fileName, roots
                )
            }
    }

    /**
     * Whether a fully-qualified class name sits under one of the given packages
     *
     * @param className Fully-qualified class name
     * @param packages Package names to test against
     * @return True when the class is in one of them or a sub-package of one
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun isUnderAnyPackage(className: String, packages: Set<String>): Boolean {
        val pkg = className.substringBeforeLast('.', "")
        return packages.any { pkg == it || pkg.startsWith("$it.") }
    }

    /**
     * Text naming the classes that could not be loaded, or empty when there were none
     *
     * @param failures Binary names of classes that could not be loaded
     * @return Suffix to append to a failure message
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun unloadableNote(failures: List<String>): String =
        if (failures.isEmpty()) "" else
            " (${failures.size} class(es) could not be loaded: ${failures.joinToString(", ")})"

    private fun readDescriptor(jarPath: Path): PluginDescriptor {
        val json = readJarEntry(jarPath, DESCRIPTOR_ENTRY)
            ?: throw PluginInstallException("Missing $DESCRIPTOR_ENTRY in plugin jar")
        return try {
            val node = objectMapper.readTree(json)
            val versionName = node.path("versionName")
            if (versionName.isMissingNode || versionName.isNull || versionName.asString().isBlank()) {
                throw PluginInstallException(
                    "Field 'versionName' is required in $DESCRIPTOR_ENTRY — set it explicitly " +
                            "when authoring the descriptor by hand (the Gradle plugin fills it from the project version)"
                )
            }
            // Checked rather than left to the deserializer, which would report a missing field
            // as a parse failure with nothing to say about which one. A descriptor without a
            // version code is refused instead of being read as 1, since that value decides
            // whether the install is allowed to replace one already present
            val versionCode = node.path("versionCode")
            if (!versionCode.isInt || versionCode.asInt() < 1) {
                throw PluginInstallException(
                    "Field 'versionCode' is required in $DESCRIPTOR_ENTRY and must be at least 1 — " +
                            "set it explicitly when authoring the descriptor by hand " +
                            "(the Gradle plugin takes it from apiPlugin { versionCode })"
                )
            }
            objectMapper.treeToValue(node, PluginDescriptor::class.java)
        } catch (e: PluginInstallException) {
            throw e
        } catch (e: Exception) {
            throw PluginInstallException("Invalid $DESCRIPTOR_ENTRY: ${e.message}")
        }
    }

    private fun readJarEntry(jarPath: Path, name: String): String? =
        JarFile(jarPath.toFile()).use { jarFile ->
            jarFile.getJarEntry(name)?.let { entry ->
                jarFile.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
            }
        }

    private fun upsertPluginRow(
        descriptor: PluginDescriptor,
        fileHash: String,
        jarName: String,
        signerKeyId: String,
        openapi: String?,
        configSchema: String?
    ) {
        val existing = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, descriptor.pluginId))
        if (existing == null) {
            saveOrThrowException {
                save(ApiPlugin().apply {
                    this.pluginId = descriptor.pluginId
                    this.name = descriptor.name
                    this.description = descriptor.description
                    // A plugin is only ever first installed by an administrator asking for it to
                    // be run, so the only enable a fresh row can have is 1. A plugin that was
                    // disabled and is then replaced keeps its 0 through the branch below - see
                    // there
                    this.enable = 1
                    this.source = "UPLOADED"
                    this.versionName = descriptor.versionName
                    this.versionCode = descriptor.versionCode
                    this.fileHash = fileHash
                    this.jarName = jarName
                    this.signerKeyId = signerKeyId
                    this.openapi = openapi
                    this.configSchema = configSchema
                })
            }
        } else {
            // What the administrator owns is left exactly as it was: enable, so that a mount
            // placed over this row - a replacement, a reload, a configuration-driven remount -
            // neither resurrects a plugin they disabled nor disables a running one, and the
            // default price, rate limit and access mode, which only the update API writes
            existing.name = descriptor.name
            existing.description = descriptor.description
            existing.source = "UPLOADED"
            existing.versionName = descriptor.versionName
            existing.versionCode = descriptor.versionCode
            existing.fileHash = fileHash
            existing.jarName = jarName
            existing.signerKeyId = signerKeyId
            existing.openapi = openapi
            existing.configSchema = configSchema
            updateOrThrowException { updateById(existing) }
        }
    }

    private fun upsertInterface(
        pluginId: String, code: String, name: String, description: String,
        fullPath: String, httpMethod: String, version: Int
    ) {
        val existing = apiInterfaceMapper.selectOne(
            KtQueryWrapper(ApiInterface())
                .eq(ApiInterface::pluginId, pluginId)
                .eq(ApiInterface::path, fullPath)
                .eq(ApiInterface::method, httpMethod)
        )

        if (existing == null) {
            saveOrThrowException {
                apiInterfaceMapper.insert(
                    ApiInterface().apply {
                        this.pluginId = pluginId
                        this.code = code
                        this.name = name
                        this.description = description
                        this.path = fullPath
                        this.method = httpMethod
                        this.apiVersion = version
                        this.billingMode = ApiInterface.BillingMode.SUCCESS_ONLY
                        this.needKey = 1
                        this.enable = 1
                    }
                ) > 0
            }
        } else {
            existing.code = code
            existing.name = name
            existing.description = description
            updateOrThrowException { apiInterfaceMapper.updateById(existing) > 0 }
        }
    }

    private fun cleanupPermissionTree(pluginId: String, codes: List<String>) {
        codes.forEach { code ->
            val operation = operationMapper.selectOne(KtQueryWrapper(Operation()).eq(Operation::code, code))
                ?: return@forEach
            val operationId = operation.id ?: return@forEach
            powerMapper.delete(KtQueryWrapper(Power()).eq(Power::id, operationId))
            operationMapper.deleteById(operationId)
        }

        val menu = menuMapper.selectOne(
            KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId)
        ) ?: return
        val scopes = scopeMapper.selectList(KtQueryWrapper(Scope()).eq(Scope::menuId, menu.id))
        scopes.forEach { scope ->
            val remaining = operationMapper.selectCount(KtQueryWrapper(Operation()).eq(Operation::scopeId, scope.id))
            if (remaining == 0L) {
                powerMapper.delete(KtQueryWrapper(Power()).eq(Power::id, scope.id))
                scopeMapper.deleteById(scope.id)
            }
        }
        val remainingScopes = scopeMapper.selectCount(KtQueryWrapper(Scope()).eq(Scope::menuId, menu.id))
        if (remainingScopes == 0L) {
            powerMapper.delete(KtQueryWrapper(Power()).eq(Power::id, menu.id))
            menuMapper.deleteById(menu.id)
        }
    }

    /**
     * Find the plugin's lifecycle entry point among the beans its context registered
     *
     * @param childContext Plugin child context
     * @param descriptor Plugin descriptor
     * @return The lifecycle implementation, or null when the plugin declares none
     * @throws PluginInstallException when `mainClass` names no bean, or several beans exist and
     *         none is named
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun findLifecycle(childContext: GenericApplicationContext, descriptor: PluginDescriptor): PluginLifecycle? {
        val lifecycleBeans = childContext.getBeansOfType(PluginLifecycle::class.java).values
        if (lifecycleBeans.isEmpty()) {
            return null
        }
        val mainClass = descriptor.mainClass
        if (mainClass != null) {
            return lifecycleBeans.firstOrNull { AopUtils.getTargetClass(it).name == mainClass }
                ?: throw PluginInstallException(
                    "Plugin '${descriptor.pluginId}' declares mainClass '$mainClass' in $DESCRIPTOR_ENTRY, " +
                            "but its context registered no PluginLifecycle bean of that name"
                )
        }
        if (lifecycleBeans.size > 1) {
            throw PluginInstallException(
                "Plugin '${descriptor.pluginId}' registered ${lifecycleBeans.size} PluginLifecycle beans " +
                        "and declares no mainClass in $DESCRIPTOR_ENTRY: set 'mainClass' to say which one " +
                        "is the entry point"
            )
        }
        return lifecycleBeans.first()
    }

    private fun ensureMenu(pluginId: String, name: String, parentId: Long, moduleId: Long): Menu {
        // Already linked to this plugin → reuse (idempotent across upgrades / restarts)
        menuMapper.selectOne(KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId))?.let { return it }

        val menu = Menu().apply {
            this.pluginId = pluginId
            this.name = name
            this.parentId = parentId
            this.moduleId = moduleId
        }
        saveOrThrowException { menuMapper.insert(menu) > 0 }
        saveOrThrowException { powerMapper.insert(Power().apply { id = menu.id; typeId = POWER_TYPE_MENU }) > 0 }

        return menu
    }

    private fun ensureScope(name: String, menuId: Long): Scope {
        val existing = scopeMapper.selectOne(KtQueryWrapper(Scope()).eq(Scope::name, name).eq(Scope::menuId, menuId))
        if (existing != null) {
            return existing
        }

        val scope = Scope().apply {
            this.name = name
            this.menuId = menuId
        }
        saveOrThrowException { scopeMapper.insert(scope) > 0 }
        saveOrThrowException { powerMapper.insert(Power().apply { id = scope.id; typeId = POWER_TYPE_SCOPE }) > 0 }

        return scope
    }

    private fun upsertOperation(code: String, name: String, scopeId: Long) {
        val existing = operationMapper.selectOne(KtQueryWrapper(Operation()).eq(Operation::code, code))
        if (existing == null) {
            val operation = Operation().apply {
                this.code = code
                this.name = name
                this.scopeId = scopeId
            }
            saveOrThrowException { operationMapper.insert(operation) > 0 }
            saveOrThrowException {
                powerMapper.insert(Power().apply {
                    id = operation.id; typeId = POWER_TYPE_OPERATION
                }) > 0
            }
        } else {
            existing.name = name
            existing.scopeId = scopeId
            operationMapper.updateById(existing)
        }
    }

    private fun refreshCache() {
        val interfaces = mutableMapOf<String, ApiInterface>()
        apiInterfaceMapper.selectList(null).forEach { apiInterface ->
            apiInterface.code?.let { interfaces[it] = apiInterface }
        }

        val plugins = mutableMapOf<String, ApiPlugin>()
        val schemas = mutableMapOf<String, PluginConfigSchema?>()
        list().forEach { plugin ->
            plugin.pluginId?.let {
                plugins[it] = plugin
                // Parsed here rather than per plugin request: the plugin context resolves
                // config defaults on the hot path of every call it serves
                schemas[it] = parseConfigSchemaOrNull(plugin.configSchema)
            }
        }

        // Both are swapped in once they are complete, never emptied and refilled: a reader
        // that arrived half way through would find no interface where one belongs and answer
        // the request as if the API were disabled. See PluginRegistryCache and
        // PluginConfigSchemaCache for what each one is read for
        pluginRegistryCache.replaceAll(interfaces, plugins)
        pluginConfigSchemaCache.replaceAll(schemas)
    }

    /**
     * Parse a declared config schema, refusing one the gateway cannot honour
     *
     * @param json Raw schema JSON read from the jar
     * @return Parsed schema, or null when the plugin declares none
     * @throws PluginInstallException when the declared schema is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun parseConfigSchema(json: String?): PluginConfigSchema? =
        try {
            PluginConfigSchemaUtil.parse(json)
        } catch (e: IllegalArgumentException) {
            throw PluginInstallException("Invalid plugin config schema: ${e.message}")
        }

    /**
     * Parse a stored config schema, tolerating one that no longer parses
     *
     * @param json Stored schema JSON
     * @return Parsed schema, or null when absent or unreadable
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun parseConfigSchemaOrNull(json: String?): PluginConfigSchema? =
        runCatching { PluginConfigSchemaUtil.parse(json) }
            .onFailure { logger.warn("Ignoring an unreadable plugin config schema: {}", it.message) }
            .getOrNull()

    private fun getByPluginIdOrQuery(pluginId: String): ApiPlugin? =
        getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))

    private fun setLoadError(pluginId: String, message: String?) {
        runCatching {
            // Set explicitly rather than through an entity: a null field is left out of the
            // statement the default strategy builds, so an entity carrying no message would
            // "clear" a column it never mentions - which is why the other nullable columns are
            // cleared this way too (see UserServiceImpl)
            updateOrThrowException {
                update(
                    KtUpdateWrapper(ApiPlugin())
                        .eq(ApiPlugin::pluginId, pluginId)
                        .set(ApiPlugin::loadError, message)
                )
            }
        }
    }

    private fun clearLoadError(pluginId: String) = setLoadError(pluginId, null)
}
