package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
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
 * Owns the plugin registry and the hot-pluggable lifecycle: upload → verify
 * signature + trust → load into a child-first class loader and a child Spring
 * context → register request mappings → upsert database rows and the permission
 * tree → refresh the in-memory caches. Plugins are re-mounted from the blob store
 * at startup ([loadUploadedPlugins]).
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JsonMapper
 * @see ApplicationContext
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
 * @see PluginConfigSchemaCache
 * @see PluginSettingReader
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
    private val pluginConfigSchemaCache: PluginConfigSchemaCache,
    private val pluginSettingReader: PluginSettingReader
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
         * Plugin ID format: lowercase letter, then lowercase letters / digits / hyphens
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private val PLUGIN_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

        /**
         * Maximum accepted plugin jar size (50 MB)
         */
        private const val MAX_JAR_SIZE = 50L * 1024 * 1024

        /**
         * Jar entry path of the plugin descriptor
         */
        private const val DESCRIPTOR_ENTRY = "META-INF/api-plugin.json"

        /**
         * Jar entry path of the embedded OpenAPI fragment
         */
        private const val OPENAPI_ENTRY = "META-INF/plugin-openapi.json"

        /**
         * Jar entry path of the declared plugin config schema
         */
        private const val CONFIG_ENTRY = "META-INF/plugin-config.json"

        /**
         * Build API scoping code from the annotation and the endpoint operationId
         *
         * @param annotation API controller annotation
         * @param method Endpoint method
         * @return API scoping code
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ApiController
         */
        fun buildCode(annotation: ApiController, method: Method): String {
            val operationId = method.getAnnotation(SwaggerOperation::class.java)?.operationId
                ?.takeIf { it.isNotBlank() } ?: method.name
            return "api:${annotation.plugin}:v${annotation.version}:$operationId"
        }
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Serializes the mount lifecycle of every plugin. Install, uninstall and reload all
     * unmount and remount, and an interleaving of two of them can leave request mappings
     * registered with no runtime left to unregister them - making every request to that
     * plugin's paths fail as ambiguous. Mounting is a rare administrator operation, so
     * one coarse lock costs nothing worth measuring.
     */
    private val mountLock = Any()
    private val pluginIdMap = ConcurrentHashMap<String, ApiPlugin>()
    private val pluginRuntimes = ConcurrentHashMap<String, PluginRuntime>()
    private val interfaceCodeMap = ConcurrentHashMap<String, ApiInterface>()

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
        if (jarBytes.size > MAX_JAR_SIZE) {
            throw PluginInstallException("Plugin jar exceeds ${MAX_JAR_SIZE / 1024 / 1024} MB")
        }
        val pluginDir = Path.of(serverProperties.storage.pluginDir)
        Files.createDirectories(pluginDir)
        val fileHash = jarBytes.sha256HexString()
        val jarPath = pluginDir.resolve("$fileHash.jar")
        Files.write(jarPath, jarBytes)

        doInstall(jarPath, jarName, persistBlob = true, checkVersion = true)
    }

    override fun updatePlugin(apiPluginUpdateParam: ApiPluginUpdateParam) {
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
    }

    override fun updatePluginStatus(apiPluginUpdateStatusParam: ApiPluginUpdateStatusParam) {
        updateOrThrowException { updateById(apiPluginUpdateStatusParam.toEntity()) }
        refreshCache()
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

        if (datasourceChanged) {
            onCommitted { remountQuietly(pluginId) }
        }
    }

    /**
     * Read a submission as values by group
     *
     * The structural mistakes are refused here - a group named twice, a key named twice - since
     * neither can be anything but the caller's bug, while whether a group or a key is declared
     * at all is the schema's business and is where `PluginConfigSchemaUtil.validate` reads it.
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

    override fun getByPluginId(pluginId: String): ApiPlugin? = pluginIdMap[pluginId]

    override fun getByCode(code: String): ApiInterface? = interfaceCodeMap[code]

    override fun listEnabledInterfaces(): List<ApiInterface> =
        apiInterfaceMapper.selectList(KtQueryWrapper(ApiInterface()).eq(ApiInterface::enable, 1))

    override fun resolveAccessMode(api: ApiInterface): ApiInterface.AccessMode =
        api.accessMode ?: getByPluginId(api.pluginId ?: "")?.defaultAccessMode
        ?: ApiInterface.AccessMode.RESTRICTED

    /**
     * Run something once the transaction around the caller has committed
     *
     * A change to a datasource is only worth acting on once it is stored, and acting on it
     * means remounting - which re-reads the settings it applies and may record a failure in
     * `load_error`. Both have to happen outside the transaction: inside it the remount would
     * hold a database connection for as long as it takes, and the failure it wrote would be
     * rolled back together with the values it was reacting to.
     */
    private fun onCommitted(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = action()
        })
    }

    /**
     * Resolve the declared config schema of a plugin
     *
     * @param pluginId Plugin ID
     * @return Parsed schema, or null when the plugin declares no configuration
     * @throws NoRecordFoundException when no such plugin is installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    private fun configSchemaOf(pluginId: String): PluginConfigSchema? {
        if (getByPluginId(pluginId) == null) {
            throw NoRecordFoundException()
        }

        return pluginConfigSchemaCache.get(pluginId)
    }

    /**
     * Remount a plugin whose configuration changed
     *
     * Runs after the configuration has been committed, so it holds no transaction and
     * cannot roll the administrator's values back: a remount that fails because of them
     * leaves the values stored and the reason in `load_error`, which is where the
     * administrator can see it and correct it. Rolling the values back instead would
     * discard the very thing that has to be edited to fix the problem.
     *
     * A plugin whose code the gateway does not own - a built-in one - has no jar to
     * remount from; its configuration reaches it when the gateway next starts.
     */
    private fun remountQuietly(pluginId: String) {
        val plugin = getByPluginIdOrQuery(pluginId) ?: return
        if (plugin.source != "UPLOADED") {
            logger.debug(
                "Remount after a configuration change skipped for plugin '{}': its source is '{}'",
                pluginId,
                plugin.source
            )

            return
        }

        synchronized(mountLock) {
            runCatching { remount(pluginId) }.onFailure {
                logger.warn("Failed to remount plugin '{}' after a configuration change: {}", pluginId, it.message)
            }
        }
    }

    /**
     * Remount an installed plugin from the jar its row points at
     *
     * The caller holds [mountLock].
     */
    private fun remount(pluginId: String): ApiPluginVo {
        val plugin = getByPluginIdOrQuery(pluginId)
            ?: throw PluginInstallException("Plugin not found: $pluginId")

        val fileHash = plugin.fileHash?.takeIf { it.isNotBlank() }
            ?: throw PluginInstallException("Plugin has no jar file hash and cannot be reloaded")
        val jarName = plugin.jarName ?: "$pluginId.jar"

        return try {
            // Everything that can fail cheaply is checked while the running plugin is
            // still serving, so a blob that vanished or a jar that is no longer the
            // plugin's own never costs availability.
            val bytes = storageBlobService.loadFile(fileHash)
                ?: throw PluginInstallException("Plugin jar blob not found for hash $fileHash")
            val pluginDir = Path.of(serverProperties.storage.pluginDir)
            Files.createDirectories(pluginDir)
            val jarPath = pluginDir.resolve("$fileHash.jar")
            Files.write(jarPath, bytes)
            val descriptor = readDescriptor(jarPath)
            if (descriptor.pluginId != pluginId) {
                throw PluginInstallException(
                    "Plugin jar blob belongs to '${descriptor.pluginId}', not '$pluginId'"
                )
            }

            // The old runtime must be gone before the new mappings are registered: two
            // live mappings for the same path make the dispatcher reject every request
            // to that path as ambiguous. Its jar survives because that is the very file
            // the new mount reads from.
            pluginRuntimes.remove(pluginId)?.let { unmount(it, deleteJar = false) }

            doInstall(jarPath, jarName, persistBlob = false, checkVersion = false, preserveRegistration = true)
        } catch (e: Exception) {
            logger.error("Failed to remount plugin '{}': {}", pluginId, e.message, e)
            // The row survives a failed remount, so from here on it claims to be installed
            // while no route is registered. load_error is the only channel the gateway
            // has to publish that, and it reaches the administrator through the plugin
            // list. setLoadError writes the row alone, so the cache is refreshed here too.
            setLoadError(pluginId, e.message ?: "Unknown error")
            refreshCache()

            throw e
        }
    }

    /**
     * Tear a plugin down
     *
     * @param pluginId Plugin ID
     * @param purgeData Whether everything the plugin owns - its settings, its datasource
     *        configuration and data, and the files it stored - should be deleted along
     *        with it.
     *        An installation that replaces an older version of the same plugin, or an
     *        uninstall the administrator asked to keep the data of, must pass false,
     *        otherwise the plugin's data would not survive its own upgrade and a
     *        reinstall could never pick up where it left off
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun removePlugin(pluginId: String, purgeData: Boolean) {
        val plugin = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId)) ?: return

        val runtime = pluginRuntimes.remove(pluginId)
        val codes = apiInterfaceMapper.selectList(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
        ).mapNotNull { it.code }

        runtime?.let { unmount(it) }

        updateOrThrowException { removeById(plugin.id) }
        apiInterfaceMapper.delete(KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId))
        cleanupPermissionTree(pluginId, codes)
        plugin.fileHash?.let { runCatching { storageBlobService.removeFile(it) } }
        refreshCache()

        runtime?.lifecycle?.let { runCatching { it.onUninstall(runtime.pluginContext) } }

        // Purged after onUninstall so a plugin flushing state while it tears down still
        // has a namespace to write into. A failure here is logged rather than raised:
        // the database rows are already gone, so aborting would only leave a
        // half-uninstalled plugin behind. Each kind is attempted on its own, so one
        // failure does not spare the others.
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
     * Re-mount every uploaded plugin from the blob store at startup. A failing
     * plugin is skipped and its error recorded; it never blocks gateway startup.
     */
    private fun loadUploadedPlugins() {
        val plugins = list(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::source, "UPLOADED"))
        plugins.forEach { plugin ->
            val pluginId = plugin.pluginId ?: return@forEach
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
     * @param checkVersion Whether the declared version must beat the installed one
     * @param preserveRegistration Whether the plugin's existing registration must
     *        survive a failure. A remount of an already-installed plugin passes true:
     *        its rows existed before this call and are still its only record, so the
     *        rollback that protects a fresh install would instead destroy a healthy
     *        plugin
     * @return Installed plugin information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Path
     * @see ApiPluginVo
     */
    private fun doInstall(
        jarPath: Path,
        jarName: String,
        persistBlob: Boolean,
        checkVersion: Boolean,
        preserveRegistration: Boolean = false
    ): ApiPluginVo {
        var mountedLoader: URLClassLoader? = null
        var mountedContext: GenericApplicationContext? = null
        var mountedDatasources = emptyMap<String, DataSource>()
        var registeredMappings = emptyList<RequestMappingInfo>()
        var savedFileHash: String? = null
        var runtime: PluginRuntime?
        var installedPluginId: String? = null
        var dbRowsCreated = false
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
            installedPluginId = descriptor.pluginId
            require(PLUGIN_ID_REGEX.matches(descriptor.pluginId)) {
                "Invalid plugin ID: '${descriptor.pluginId}' (must match $PLUGIN_ID_REGEX)"
            }
            val loader = PluginClassLoaderManager.create(jarPath.toUri().toURL())
            mountedLoader = loader
            val controllerClasses = scanControllers(loader, jarPath)
            if (controllerClasses.isEmpty()) {
                throw PluginInstallException("No @ApiController class found in jar")
            }
            val pluginIds = controllerClasses.mapNotNull { it.getAnnotation(ApiController::class.java)?.plugin }.toSet()
            if (pluginIds.size != 1 || pluginIds.first() != descriptor.pluginId) {
                throw PluginInstallException("All controllers must share the plugin id '${descriptor.pluginId}'")
            }

            // ---- 3. upgrade rule ----
            val existing = getByPluginIdOrQuery(descriptor.pluginId)
            // An upgrade removes the row below and has the install rebuild it, so the
            // administrator's enable flag has to be carried across by hand - see
            // upsertPluginRow. Reading it before the removal is the whole point: afterwards
            // there is nothing left to read it from
            val previousEnable = existing?.enable
            if (checkVersion && existing != null) {
                val currentCode = existing.versionCode ?: 0
                if (descriptor.versionCode <= currentCode) {
                    throw PluginVersionConflictException(
                        "Version code ${descriptor.versionCode} is not greater than current $currentCode"
                    )
                }
                // An upgrade replaces the code, never the plugin's own data: its settings,
                // its datasource configuration and its stored files all outlive the version
                removePlugin(descriptor.pluginId, purgeData = false)
            }

            // ---- 4. persist jar into the blob store ----
            val fileHash = Files.readAllBytes(jarPath).sha256HexString()
            if (persistBlob) {
                savedFileHash = storageBlobService.saveFile(Files.readAllBytes(jarPath))
            }
            val configSchemaJson = readJarEntry(jarPath, CONFIG_ENTRY)
            val configSchema = parseConfigSchema(configSchemaJson)

            // ---- 5. child context + beans ----
            // Declaring a datasource is what earns a plugin one: without the declaration
            // there is nothing for the administrator to configure and nothing for the
            // gateway to inject, whatever rows happen to be left over. A declared one that
            // is not configured is simply absent - see PluginDatasourceFactory.create
            val datasources = pluginDatasourceFactory.create(descriptor.pluginId, configSchema)
            mountedDatasources = datasources
            val pluginStorage = pluginStorageFactory.create(descriptor.pluginId)
            val childContext = createChildContext(
                jarPath, loader, controllerClasses, descriptor.pluginId, datasources, pluginStorage
            )
            mountedContext = childContext
            val controllers = controllerClasses.map { childContext.getBean(it) }

            // ---- 6. register request mappings ----
            val endpoints = registerMappings(controllers)
            registeredMappings = endpoints.map { it.info }

            // ---- 7. database rows + permission tree ----
            upsertPluginRow(
                descriptor, fileHash, jarName, signerKeyId,
                readJarEntry(jarPath, OPENAPI_ENTRY), configSchemaJson,
                enable = previousEnable ?: 1
            )
            dbRowsCreated = true
            val pluginMenu = ensureMenu(descriptor.pluginId, descriptor.name, API_ROOT_MENU_ID, API_MODULE_ID)
            val scopeIds = ConcurrentHashMap<String, Long>()
            endpoints.forEach { endpoint ->
                val scopeId = scopeIds.getOrPut("v${endpoint.apiVersion}") {
                    ensureScope("v${endpoint.apiVersion}", pluginMenu.id!!).id!!
                }
                upsertInterface(
                    descriptor.pluginId, endpoint.code, endpoint.name, endpoint.description,
                    endpoint.fullPath, endpoint.httpMethod, endpoint.apiVersion
                )
                upsertOperation(endpoint.code, endpoint.name, scopeId)
            }
            refreshCache()

            // ---- 8. lifecycle + runtime record ----
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
            runtime = PluginRuntime(
                pluginId = descriptor.pluginId,
                classLoader = loader,
                context = childContext,
                pluginContext = pluginContext,
                controllerBeans = controllers,
                endpoints = endpoints,
                tempJarPath = jarPath,
                fileHash = fileHash,
                datasources = datasources,
                signerKeyId = signerKeyId,
                lifecycle = lifecycle
            )
            pluginRuntimes[descriptor.pluginId] = runtime
            lifecycle?.onInstall(pluginContext)
            lifecycle?.onStart(pluginContext)
            // The row alone, and the cache was refreshed before this: a list reads a plugin out
            // of the cache, so clearing an error without refreshing afterwards would leave a
            // mounted plugin reported as a failed one until something else happened to refresh
            clearLoadError(descriptor.pluginId)
            refreshCache()

            return getByPluginIdOrQuery(descriptor.pluginId)
                ?.toVo()
                ?: throw PluginInstallException("Failed to query the installed plugin")
        } catch (e: Exception) {
            registeredMappings.forEach { runCatching { requestMappingHandlerMapping.unregisterMapping(it) } }
            mountedContext?.let { runCatching { it.close() } }
            // Closed here as well as on unmount: a mount that failed after the pools were
            // built would otherwise leave a pool and its threads behind for good, since no
            // runtime is ever recorded for a mount that did not finish
            closeDatasources(mountedDatasources)
            mountedLoader?.let { runCatching { it.close() } }
            savedFileHash?.let { runCatching { storageBlobService.removeFile(it) } }
            runCatching { Files.deleteIfExists(jarPath) }
            // Roll back database rows created during the failed install so no orphaned
            // plugin / interface rows remain. A remount must never run this: the rows it
            // would delete are the record of a plugin that existed before the call, and
            // deleting them would turn a failed configuration change into a silent
            // uninstall.
            val pluginId = installedPluginId
            if (pluginId != null && dbRowsCreated && !preserveRegistration) {
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
            throw e
        }
    }

    /**
     * Tear the runtime of a plugin down
     *
     * @param runtime Runtime to tear down
     * @param deleteJar Whether the local jar file should be deleted with it. A remount
     *        passes false: the jar is content-addressed and is the very file the new
     *        mount reads from
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginRuntime
     */
    private fun unmount(runtime: PluginRuntime, deleteJar: Boolean = true) {
        runtime.endpoints.forEach { runCatching { requestMappingHandlerMapping.unregisterMapping(it.info) } }
        runtime.lifecycle?.let { runCatching { it.onStop(runtime.pluginContext) } }
        runCatching { runtime.context.close() }
        // Closed after the context, so a lifecycle bean shutting down can still use its
        // connection, and before the class loader, because Hikari's housekeeper and
        // connection-adder threads reach into the driver classes that loader owns. Hikari's
        // close is idempotent, so a second close from another path stays harmless.
        closeDatasources(runtime.datasources)
        runCatching { runtime.classLoader.close() }
        if (deleteJar) {
            runCatching { Files.deleteIfExists(runtime.tempJarPath) }
        }
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
        // Scan the root packages that lead to the plugin's controllers so collaborators
        // (e.g. @Service beans) living anywhere under those roots are picked up, while
        // a fat jar's bundled-library components are left alone.
        val controllerPackages = controllerClasses.map { it.`package`.name }.toSet()
        val roots = PluginClassLoaderManager.rootPackages(jarPath)
            .filter { root -> controllerPackages.any { it == root || it.startsWith("$root.") } }
        if (roots.isEmpty()) {
            throw PluginInstallException("Cannot derive scan packages for plugin $pluginId")
        }
        scanner.scan(*roots.toTypedArray())

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

    private fun registerMappings(controllers: List<Any>): List<MountedEndpoint> {
        val endpoints = mutableListOf<MountedEndpoint>()
        controllers.forEach { controller ->
            val targetClass = AopUtils.getTargetClass(controller)
            val annotation = targetClass.getAnnotation(ApiController::class.java)
                ?: throw PluginInstallException("${targetClass.name} is not annotated with @ApiController")
            val basePath = annotation.path.firstOrNull()?.trim('/') ?: ""
            targetClass.declaredMethods.forEach { method ->
                val mapping = resolveMapping(method) ?: return@forEach
                val operation = method.getAnnotation(SwaggerOperation::class.java)
                val methodPath = mapping.path.trim('/')
                val fullPath = buildString {
                    append("/api/").append(annotation.plugin).append("/v").append(annotation.version)
                    if (basePath.isNotEmpty()) append("/").append(basePath)
                    if (methodPath.isNotEmpty()) append("/").append(methodPath)
                }
                val templatePath = buildString {
                    append("/api/{PLUGIN}/v{API_VERSION}")
                    if (basePath.isNotEmpty()) append("/").append(basePath)
                    if (methodPath.isNotEmpty()) append("/").append(methodPath)
                }
                val info = RequestMappingInfo
                    .paths(templatePath)
                    .methods(mapping.method)
                    .produces(*mapping.produces.toTypedArray())
                    .consumes(*mapping.consumes.toTypedArray())
                    .customCondition(ApiVersionCondition(annotation.plugin, annotation.version))
                    .build()
                requestMappingHandlerMapping.registerMapping(info, controller, method)

                endpoints += MountedEndpoint(
                    controller = controller,
                    method = method,
                    code = buildCode(annotation, method),
                    name = operation?.summary?.takeIf { it.isNotBlank() } ?: method.name,
                    description = operation?.description?.takeIf { it.isNotBlank() } ?: "",
                    fullPath = fullPath,
                    httpMethod = mapping.method.name,
                    apiVersion = annotation.version,
                    info = info
                )
            }
        }
        return endpoints
    }

    private data class EndpointMapping(
        val path: String,
        val method: RequestMethod,
        val produces: List<String>,
        val consumes: List<String>
    )

    private fun resolveMapping(method: Method): EndpointMapping? {
        val get = method.getAnnotation(GetMapping::class.java)
        val post = method.getAnnotation(PostMapping::class.java)
        val put = method.getAnnotation(PutMapping::class.java)
        val patch = method.getAnnotation(PatchMapping::class.java)
        val delete = method.getAnnotation(DeleteMapping::class.java)

        return when {
            get != null -> EndpointMapping(
                (get.path.ifEmpty { get.value }).firstOrNull() ?: "",
                RequestMethod.GET, get.produces.toList(), get.consumes.toList()
            )

            post != null -> EndpointMapping(
                (post.path.ifEmpty { post.value }).firstOrNull() ?: "",
                RequestMethod.POST, post.produces.toList(), post.consumes.toList()
            )

            put != null -> EndpointMapping(
                (put.path.ifEmpty { put.value }).firstOrNull() ?: "",
                RequestMethod.PUT, put.produces.toList(), put.consumes.toList()
            )

            patch != null -> EndpointMapping(
                (patch.path.ifEmpty { patch.value }).firstOrNull() ?: "",
                RequestMethod.PATCH, patch.produces.toList(), patch.consumes.toList()
            )

            delete != null -> EndpointMapping(
                (delete.path.ifEmpty { delete.value }).firstOrNull() ?: "",
                RequestMethod.DELETE, delete.produces.toList(), delete.consumes.toList()
            )

            else -> null
        }
    }

    private fun scanControllers(loader: URLClassLoader, jarPath: Path): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        JarFile(jarPath.toFile()).use { jarFile ->
            jarFile.entries().asSequence().forEach { entry ->
                if (entry.isDirectory || !entry.name.endsWith(".class")) return@forEach
                val className = entry.name.removeSuffix(".class").replace('/', '.')
                if (className.contains('$')) return@forEach
                try {
                    val clazz = loader.loadClass(className)
                    if (clazz.getAnnotation(ApiController::class.java) != null) {
                        result.add(clazz)
                    }
                } catch (_: Throwable) {
                    // skip classes that cannot be loaded (optional plugin-internal helpers)
                }
            }
        }
        return result
    }

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
        configSchema: String?,
        enable: Int = 1
    ) {
        val existing = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, descriptor.pluginId))
        if (existing == null) {
            saveOrThrowException {
                save(ApiPlugin().apply {
                    this.pluginId = descriptor.pluginId
                    this.name = descriptor.name
                    this.description = descriptor.description
                    this.enable = enable
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
            // enable is deliberately untouched here, so a remount leaves a disabled plugin
            // disabled. An upgrade arrives through the insert above instead - it removes
            // the row first - which is why that branch takes the flag as a parameter
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

    private fun findLifecycle(childContext: GenericApplicationContext, descriptor: PluginDescriptor): PluginLifecycle? {
        val lifecycleBeans = childContext.getBeansOfType(PluginLifecycle::class.java).values
        if (lifecycleBeans.isEmpty()) {
            return null
        }
        descriptor.mainClass?.let { main ->
            lifecycleBeans.firstOrNull { it.javaClass.name == main }?.let { return it }
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
        interfaceCodeMap.clear()
        pluginIdMap.clear()
        apiInterfaceMapper.selectList(null).forEach { apiInterface ->
            apiInterface.code?.let { interfaceCodeMap[it] = apiInterface }
        }
        val schemas = mutableMapOf<String, PluginConfigSchema?>()
        list().forEach { plugin ->
            plugin.pluginId?.let {
                pluginIdMap[it] = plugin
                // Parsed here rather than per plugin request: the plugin context resolves
                // config defaults on the hot path of every call it serves
                schemas[it] = parseConfigSchemaOrNull(plugin.configSchema)
            }
        }
        // Swapped in once it is complete: adding schemas one at a time would leave a window
        // in which a plugin that has one appears to declare nothing, and a schema is what
        // says which of its stored values are secrets - see PluginConfigSchemaCache
        pluginConfigSchemaCache.replaceAll(schemas)
    }

    /**
     * Parse a declared config schema, refusing one the gateway cannot honour
     *
     * A schema that does not parse would leave the administrator editing settings the
     * plugin never reads, so it fails the install rather than being ignored.
     *
     * @param json Raw schema JSON read from the jar
     * @return Parsed schema, or null when the plugin declares none
     * @throws PluginInstallException when the declared schema is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
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
     * Used while refreshing the cache, where an unreadable schema must not take down
     * unrelated administration operations; the plugin then simply has no declared
     * configuration until it is mounted again.
     *
     * @param json Stored schema JSON
     * @return Parsed schema, or null when absent or unreadable
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
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
