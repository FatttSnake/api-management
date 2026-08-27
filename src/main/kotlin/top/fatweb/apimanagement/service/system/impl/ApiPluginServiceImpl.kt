package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.ApiController
import top.fatweb.apimanagement.converter.system.toEntity
import top.fatweb.apimanagement.converter.system.toVoPage
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.entity.system.ApiInterface
import top.fatweb.apimanagement.entity.system.ApiPlugin
import top.fatweb.apimanagement.mapper.permission.MenuMapper
import top.fatweb.apimanagement.mapper.permission.OperationMapper
import top.fatweb.apimanagement.mapper.permission.PowerMapper
import top.fatweb.apimanagement.mapper.permission.ScopeMapper
import top.fatweb.apimanagement.mapper.system.ApiInterfaceMapper
import top.fatweb.apimanagement.mapper.system.ApiPluginMapper
import top.fatweb.apimanagement.param.system.api.ApiInterfaceGetParam
import top.fatweb.apimanagement.param.system.api.ApiInterfaceUpdateParam
import top.fatweb.apimanagement.param.system.api.ApiPluginGetParam
import top.fatweb.apimanagement.param.system.api.ApiPluginUpdateParam
import top.fatweb.apimanagement.service.system.IApiPluginService
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiInterfaceVo
import top.fatweb.apimanagement.vo.system.ApiPluginVo
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import io.swagger.v3.oas.annotations.Operation as SwaggerOperation

/**
 * API plugin service implement
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
    private val menuMapper: MenuMapper,
    private val scopeMapper: ScopeMapper,
    private val operationMapper: OperationMapper,
    private val powerMapper: PowerMapper,
    private val apiInterfaceMapper: ApiInterfaceMapper
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

    private val interfaceCodeMap = ConcurrentHashMap<String, ApiInterface>()
    private val pluginIdMap = ConcurrentHashMap<String, ApiPlugin>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            registerApis()
        } catch (e: Exception) {
            if (e is IllegalArgumentException || e is IllegalStateException) {
                throw e
            }
            logger.warn("Failed to register API controllers: {}", e.message)
        }
    }

    override fun getPluginPage(apiPluginGetParam: ApiPluginGetParam?): PageVo<ApiPluginVo> {
        val page = Page<ApiPlugin>(apiPluginGetParam?.currentPage ?: 1, apiPluginGetParam?.pageSize ?: 20)
        setPageSort(apiPluginGetParam, page, OrderItem.desc("create_time"))

        val wrapper = KtQueryWrapper(ApiPlugin()).apply {
            apiPluginGetParam?.searchName?.let { like(ApiPlugin::name, it) }
            apiPluginGetParam?.enabled?.let { eq(ApiPlugin::enabled, if (it) 1 else 0) }
        }

        return page(page, wrapper).toVoPage()
    }

    override fun updatePlugin(apiPluginUpdateParam: ApiPluginUpdateParam) {
        updateOrThrowException { updateById(apiPluginUpdateParam.toEntity()) }
        refreshCache()
    }

    override fun getInterfacePage(apiInterfaceGetParam: ApiInterfaceGetParam?): PageVo<ApiInterfaceVo> {
        val page = Page<ApiInterface>(apiInterfaceGetParam?.currentPage ?: 1, apiInterfaceGetParam?.pageSize ?: 20)
        setPageSort(apiInterfaceGetParam, page, OrderItem.desc("create_time"))

        val wrapper = KtQueryWrapper(ApiInterface()).apply {
            apiInterfaceGetParam?.searchCode?.let { like(ApiInterface::code, it) }
            apiInterfaceGetParam?.searchName?.let { like(ApiInterface::name, it) }
            apiInterfaceGetParam?.pluginId?.let { eq(ApiInterface::pluginId, it) }
            apiInterfaceGetParam?.enabled?.let { eq(ApiInterface::enabled, if (it) 1 else 0) }
        }

        return apiInterfaceMapper.selectPage(page, wrapper).toVoPage()
    }

    override fun updateInterface(apiInterfaceUpdateParam: ApiInterfaceUpdateParam) {
        updateOrThrowException { apiInterfaceMapper.updateById(apiInterfaceUpdateParam.toEntity()) > 0 }
        refreshCache()
    }

    override fun getByCode(code: String): ApiInterface? = interfaceCodeMap[code]

    override fun getByPluginId(pluginId: String): ApiPlugin? = pluginIdMap[pluginId]

    override fun listEnabledInterfaces(): List<ApiInterface> =
        apiInterfaceMapper.selectList(KtQueryWrapper(ApiInterface()).eq(ApiInterface::enabled, 1))

    override fun registerApis() {
        val byPlugin = applicationContext.getBeansWithAnnotation<ApiController>()
            .values
            .mapNotNull { bean ->
                val targetClass = AopUtils.getTargetClass(bean)
                val annotation = targetClass.getAnnotation(ApiController::class.java) ?: return@mapNotNull null
                annotation to targetClass
            }
            .groupBy { it.first.plugin }

        byPlugin.forEach { (plugin, controllers) ->
            require(PLUGIN_ID_REGEX.matches(plugin)) {
                "Invalid plugin ID: '$plugin' (must match $PLUGIN_ID_REGEX)"
            }
            val versions = controllers.map { it.first.version }
            check(versions.distinct().size == versions.size) {
                "Duplicate versions for plugin '$plugin': ${versions.sorted()}"
            }
        }

        byPlugin.forEach { (plugin, controllers) ->
            val first = controllers.first().first
            upsertPlugin(plugin, first.pluginName, first.description)

            val pluginMenu = ensureMenu(first.pluginName, API_ROOT_MENU_ID, API_MODULE_ID)
            controllers.forEach { (annotation, targetClass) ->
                val version = annotation.version
                val versionScope = ensureScope("v$version", pluginMenu.id!!)

                targetClass.declaredMethods.forEach { method ->
                    val mapping = resolveMapping(method) ?: return@forEach
                    val (methodPath, httpMethod) = mapping
                    val basePath = annotation.path.firstOrNull()?.trim('/') ?: ""
                    val fullPath = buildString {
                        append("/api/").append(plugin).append("/v").append(version)
                        if (basePath.isNotEmpty()) append("/").append(basePath)
                        if (methodPath.isNotEmpty()) append("/").append(methodPath.trim('/'))
                    }
                    val code = buildCode(annotation, method)

                    upsertInterface(plugin, code, method.name, annotation.description, fullPath, httpMethod, version)
                    upsertOperation(code, method.name, versionScope.id!!)
                }
            }
        }

        refreshCache()
    }

    private fun resolveMapping(method: Method): Pair<String, String>? {
        val get = method.getAnnotation(GetMapping::class.java)
        val post = method.getAnnotation(PostMapping::class.java)
        val put = method.getAnnotation(PutMapping::class.java)
        val patch = method.getAnnotation(PatchMapping::class.java)
        val delete = method.getAnnotation(DeleteMapping::class.java)

        return when {
            get != null -> Pair((get.path.ifEmpty { get.value }).firstOrNull() ?: "", "GET")
            post != null -> Pair((post.path.ifEmpty { post.value }).firstOrNull() ?: "", "POST")
            put != null -> Pair((put.path.ifEmpty { put.value }).firstOrNull() ?: "", "PUT")
            patch != null -> Pair((patch.path.ifEmpty { patch.value }).firstOrNull() ?: "", "PATCH")
            delete != null -> Pair((delete.path.ifEmpty { delete.value }).firstOrNull() ?: "", "DELETE")
            else -> null
        }
    }

    private fun upsertPlugin(pluginId: String, name: String, description: String) {
        val existing = getOne(KtQueryWrapper(ApiPlugin()).eq(ApiPlugin::pluginId, pluginId))
        if (existing == null) {
            saveOrThrowException {
                save(
                    ApiPlugin().apply {
                        this.pluginId = pluginId
                        this.name = name
                        this.description = description
                        this.enabled = 1
                    }
                )
            }
        } else {
            existing.name = name
            existing.description = description
            updateOrThrowException { updateById(existing) }
        }
    }

    private fun ensureMenu(name: String, parentId: Long, moduleId: Long): Menu {
        val existing = menuMapper.selectOne(
            KtQueryWrapper(Menu()).eq(Menu::name, name).eq(Menu::parentId, parentId).eq(Menu::moduleId, moduleId)
        )
        if (existing != null) {
            return existing
        }

        val menu = Menu().apply {
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
                        this.enabled = 1
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
}
