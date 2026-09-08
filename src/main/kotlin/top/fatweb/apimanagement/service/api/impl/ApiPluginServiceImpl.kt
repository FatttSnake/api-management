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
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.component.api.ApiVersionCondition
import top.fatweb.apimanagement.component.plugin.MountedEndpoint
import top.fatweb.apimanagement.component.plugin.PluginClassLoaderManager
import top.fatweb.apimanagement.component.plugin.PluginContextImpl
import top.fatweb.apimanagement.component.plugin.PluginRuntime
import top.fatweb.apimanagement.converter.api.toEntity
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.exception.PluginNotTrustedException
import top.fatweb.apimanagement.exception.PluginSignatureInvalidException
import top.fatweb.apimanagement.exception.PluginVersionConflictException
import top.fatweb.apimanagement.mapper.api.ApiInterfaceMapper
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
import top.fatweb.apimanagement.mapper.permission.MenuMapper
import top.fatweb.apimanagement.mapper.permission.OperationMapper
import top.fatweb.apimanagement.mapper.permission.PowerMapper
import top.fatweb.apimanagement.mapper.permission.ScopeMapper
import top.fatweb.apimanagement.param.system.api.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.annotation.ApiController
import top.fatweb.apimanagement.sdk.plugin.PluginContext
import top.fatweb.apimanagement.sdk.plugin.PluginDescriptor
import top.fatweb.apimanagement.sdk.plugin.PluginLifecycle
import top.fatweb.apimanagement.sdk.plugin.PluginSigner
import top.fatweb.apimanagement.service.api.*
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.util.sha256HexString
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiGroupVo
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
 * @see ApplicationContext
 * @see MenuMapper
 * @see ScopeMapper
 * @see OperationMapper
 * @see PowerMapper
 * @see ServiceImpl
 * @see ApiInterfaceMapper
 * @see ApiPluginMapper
 * @see ApiPlugin
 * @see IApiPluginService
 */
@Service
@DS("master")
class ApiPluginServiceImpl(
    private val applicationContext: ApplicationContext,
    @Lazy private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val objectMapper: JsonMapper,
    private val serverProperties: ServerProperties,
    private val menuMapper: MenuMapper,
    private val scopeMapper: ScopeMapper,
    private val operationMapper: OperationMapper,
    private val powerMapper: PowerMapper,
    private val storageBlobService: IStorageBlobService,
    private val apiInterfaceMapper: ApiInterfaceMapper,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val apiPluginTrustKeyService: IApiPluginTrustKeyService,
    private val apiPluginDatasourceService: IApiPluginDatasourceService,
    @Lazy private val apiAccountService: IApiAccountService
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
         * Build API scoping code from the annotation and the endpoint operationId
         *
         * @param annotation API controller annotation
         * @param method Endpoint method
         * @return API scoping code
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ApiController
         * @see Operation
         */
        fun buildCode(annotation: ApiController, method: Method): String {
            val operationId = method.getAnnotation(SwaggerOperation::class.java)?.operationId
                ?.takeIf { it.isNotBlank() } ?: method.name
            return "api:${annotation.plugin}:v${annotation.version}:$operationId"
        }
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
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

    override fun installPlugin(jarBytes: ByteArray, jarName: String): ApiPluginVo {
        if (jarBytes.size > MAX_JAR_SIZE) {
            throw PluginInstallException("Plugin jar exceeds ${MAX_JAR_SIZE / 1024 / 1024} MB")
        }
        val pluginDir = Path.of(serverProperties.storage.pluginDir)
        Files.createDirectories(pluginDir)
        val fileHash = jarBytes.sha256HexString()
        val jarPath = pluginDir.resolve("$fileHash.jar")
        Files.write(jarPath, jarBytes)

        return doInstall(jarPath, jarName, persistBlob = true, checkVersion = true)
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

    override fun uninstallPlugin(pluginId: String) {
        val plugin = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))
            ?: throw PluginInstallException("Plugin not found: $pluginId")
        if (plugin.source != "UPLOADED") {
            throw PluginInstallException("Built-in plugins cannot be uninstalled")
        }

        val runtime = pluginRuntimes.remove(pluginId)
        val codes = apiInterfaceMapper.selectList(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId)
        ).mapNotNull { it.code }

        runtime?.let { unmount(it) }

        updateOrThrowException { removeById(plugin.id) }
        apiInterfaceMapper.delete(KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId))
        cleanupPermissionTree(pluginId, codes)
        apiPluginSettingService.deleteByPlugin(pluginId)
        plugin.fileHash?.let { runCatching { storageBlobService.removeFile(it) } }
        refreshCache()

        runtime?.lifecycle?.let { runCatching { it.onUninstall(runtime.pluginContext) } }
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
                doInstall(jarPath, plugin.jarName ?: "$pluginId.jar", persistBlob = false, checkVersion = false)
            } catch (e: Exception) {
                logger.error("Failed to load plugin '{}': {}", pluginId, e.message)
                setLoadError(pluginId, e.message ?: "Unknown error")
            }
        }
    }

    private fun doInstall(jarPath: Path, jarName: String, persistBlob: Boolean, checkVersion: Boolean): ApiPluginVo {
        var mountedLoader: URLClassLoader? = null
        var mountedContext: GenericApplicationContext? = null
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
            if (checkVersion && existing != null) {
                val currentCode = existing.versionCode ?: 0
                if (descriptor.versionCode <= currentCode) {
                    throw PluginVersionConflictException(
                        "Version code ${descriptor.versionCode} is not greater than current $currentCode"
                    )
                }
                uninstallPlugin(descriptor.pluginId)
            }

            // ---- 4. persist jar into the blob store ----
            val fileHash = Files.readAllBytes(jarPath).sha256HexString()
            if (persistBlob) {
                savedFileHash = storageBlobService.saveFile(Files.readAllBytes(jarPath))
            }

            // ---- 5. child context + beans ----
            val datasource = apiPluginDatasourceService.buildIfConfigured(descriptor.pluginId)
            val childContext = createChildContext(jarPath, loader, controllerClasses, descriptor.pluginId, datasource)
            mountedContext = childContext
            val controllers = controllerClasses.map { childContext.getBean(it) }

            // ---- 6. register request mappings ----
            val endpoints = registerMappings(controllers)
            registeredMappings = endpoints.map { it.info }

            // ---- 7. database rows + permission tree ----
            upsertPluginRow(descriptor, fileHash, jarName, signerKeyId, readJarEntry(jarPath, OPENAPI_ENTRY))
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
                datasource = datasource,
                apiAccountService = apiAccountService,
                apiPluginSettingService = apiPluginSettingService,
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
                signerKeyId = signerKeyId,
                lifecycle = lifecycle
            )
            pluginRuntimes[descriptor.pluginId] = runtime
            lifecycle?.onInstall(pluginContext)
            lifecycle?.onStart(pluginContext)
            clearLoadError(descriptor.pluginId)

            return getByPluginIdOrQuery(descriptor.pluginId)
                ?.toVo()
                ?: throw PluginInstallException("Failed to query the installed plugin")
        } catch (e: Exception) {
            registeredMappings.forEach { runCatching { requestMappingHandlerMapping.unregisterMapping(it) } }
            mountedContext?.let { runCatching { it.close() } }
            mountedLoader?.let { runCatching { it.close() } }
            savedFileHash?.let { runCatching { storageBlobService.removeFile(it) } }
            runCatching { Files.deleteIfExists(jarPath) }
            // Roll back database rows created during the failed install so no orphaned
            // plugin / interface rows remain.
            val pluginId = installedPluginId
            if (pluginId != null && dbRowsCreated) {
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

    private fun unmount(runtime: PluginRuntime) {
        runtime.endpoints.forEach { runCatching { requestMappingHandlerMapping.unregisterMapping(it.info) } }
        runtime.lifecycle?.let { runCatching { it.onStop(runtime.pluginContext) } }
        runCatching { runtime.context.close() }
        runCatching { runtime.classLoader.close() }
        runCatching { Files.deleteIfExists(runtime.tempJarPath) }
    }

    private fun createChildContext(
        jarPath: Path,
        loader: URLClassLoader,
        controllerClasses: List<Class<*>>,
        pluginId: String,
        datasource: DataSource?
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
                datasource = datasource,
                apiAccountService = apiAccountService,
                apiPluginSettingService = apiPluginSettingService,
                interfaceLookup = { getByCode(it) }
            )
        })
        datasource?.let {
            ctx.registerBean("pluginDataSource", DataSource::class.java, Supplier { it })
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
            objectMapper.readValue(json, PluginDescriptor::class.java)
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
        openapi: String?
    ) {
        val existing = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, descriptor.pluginId))
        if (existing == null) {
            saveOrThrowException {
                save(ApiPlugin().apply {
                    this.pluginId = descriptor.pluginId
                    this.name = descriptor.name
                    this.description = descriptor.description
                    this.enable = 1
                    this.source = "UPLOADED"
                    this.versionName = descriptor.versionName
                    this.versionCode = descriptor.versionCode
                    this.fileHash = fileHash
                    this.jarName = jarName
                    this.signerKeyId = signerKeyId
                    this.openapi = openapi
                })
            }
        } else {
            existing.name = descriptor.name
            existing.description = descriptor.description
            existing.enable = 1
            existing.source = "UPLOADED"
            existing.versionName = descriptor.versionName
            existing.versionCode = descriptor.versionCode
            existing.fileHash = fileHash
            existing.jarName = jarName
            existing.signerKeyId = signerKeyId
            existing.openapi = openapi
            existing.loadError = null
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
        list().forEach { plugin ->
            plugin.pluginId?.let { pluginIdMap[it] = plugin }
        }
    }

    private fun getByPluginIdOrQuery(pluginId: String): ApiPlugin? =
        getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))

    private fun setLoadError(pluginId: String, message: String?) {
        runCatching {
            val plugin = getByPluginIdOrQuery(pluginId) ?: return
            plugin.loadError = message
            updateOrThrowException { updateById(plugin) }
        }
    }

    private fun clearLoadError(pluginId: String) = setLoadError(pluginId, null)
}
