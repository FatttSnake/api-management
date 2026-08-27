package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.aop.support.AopUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.ApiController
import top.fatweb.apimanagement.converter.system.toEntity
import top.fatweb.apimanagement.converter.system.toVoPage
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.system.Api
import top.fatweb.apimanagement.mapper.permission.OperationMapper
import top.fatweb.apimanagement.mapper.permission.PowerMapper
import top.fatweb.apimanagement.mapper.system.ApiMapper
import top.fatweb.apimanagement.param.system.api.ApiGetParam
import top.fatweb.apimanagement.param.system.api.ApiUpdateParam
import top.fatweb.apimanagement.service.system.IApiService
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.util.updateOrThrowException
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiVo
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * API service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApplicationContext
 * @see OperationMapper
 * @see PowerMapper
 * @see ServiceImpl
 * @see ApiMapper
 * @see Api
 * @see IApiService
 */
@Service
@DS("master")
class ApiServiceImpl(
    private val applicationContext: ApplicationContext,
    private val operationMapper: OperationMapper,
    private val powerMapper: PowerMapper
) : ServiceImpl<ApiMapper, Api>(), IApiService, ApplicationRunner {
    companion object {
        /**
         * Scope ID of API platform registry scope (t_s_scope)
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val API_OPERATION_SCOPE_ID = 1540700L

        /**
         * Type ID of operation power (t_s_power_type)
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val POWER_TYPE_OPERATION = 4

        /**
         * Build API scoping code from annotation and method name
         *
         * @param annotation API controller annotation
         * @param methodName Method name
         * @return API scoping code
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ApiController
         */
        fun buildCode(annotation: ApiController, methodName: String): String {
            val segment = annotation.path.firstOrNull()?.trim('/')?.substringBefore('/') ?: ""
            return "api:v${annotation.version}:$segment:$methodName"
        }
    }

    private val apiCodeMap = ConcurrentHashMap<String, Api>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            registerApis()
        } catch (e: Exception) {
            logger.warn("Failed to register API controllers: {}", e.message)
        }
    }

    override fun getPage(apiGetParam: ApiGetParam?): PageVo<ApiVo> {
        val page = Page<Api>(apiGetParam?.currentPage ?: 1, apiGetParam?.pageSize ?: 20)
        setPageSort(apiGetParam, page, OrderItem.desc("create_time"))

        val wrapper = KtQueryWrapper(Api()).apply {
            apiGetParam?.searchCode?.let { like(Api::code, it) }
            apiGetParam?.searchName?.let { like(Api::name, it) }
            apiGetParam?.enabled?.let { eq(Api::enabled, if (it) 1 else 0) }
        }

        return this.page(page, wrapper).toVoPage()
    }

    override fun update(apiUpdateParam: ApiUpdateParam) {
        updateOrThrowException { this.updateById(apiUpdateParam.toEntity()) }
        refreshCache()
    }

    override fun getByCode(code: String): Api? = apiCodeMap[code]

    override fun registerApis() {
        applicationContext.getBeansWithAnnotation<ApiController>().values.forEach { bean ->
            val targetClass = AopUtils.getTargetClass(bean)
            val annotation = targetClass.getAnnotation(ApiController::class.java) ?: return@forEach
            val version = annotation.version
            val basePath = annotation.path.firstOrNull()?.trim('/') ?: ""

            targetClass.declaredMethods.forEach { method ->
                val mapping = resolveMapping(method) ?: return@forEach
                val (methodPath, httpMethod) = mapping
                val fullPath = buildString {
                    append("/api/v").append(version)
                    if (basePath.isNotEmpty()) append("/").append(basePath)
                    if (methodPath.isNotEmpty()) append("/").append(methodPath.trim('/'))
                }
                val code = buildCode(annotation, method.name)

                upsertApi(code, annotation.name, annotation.description, fullPath, httpMethod, version)
                upsertOperation(code, method.name)
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

    private fun upsertApi(
        code: String, name: String, description: String, fullPath: String, httpMethod: String, version: Int
    ) {
        val existing = this.getOne(
            KtQueryWrapper(Api()).eq(Api::path, fullPath).eq(Api::method, httpMethod)
        )

        if (existing == null) {
            saveOrThrowException {
                this.save(
                    Api().apply {
                        this.code = code
                        this.name = name
                        this.description = description
                        path = fullPath
                        method = httpMethod
                        apiVersion = version
                        billingMode = Api.BillingMode.SUCCESS_ONLY
                        needKey = 1
                        rateLimit = 0
                        enabled = 1
                    }
                )
            }
        } else {
            existing.code = code
            existing.name = name
            existing.description = description
            updateOrThrowException { this.updateById(existing) }
        }
    }

    private fun upsertOperation(code: String, name: String) {
        val existing = operationMapper.selectOne(KtQueryWrapper(Operation()).eq(Operation::code, code))
        if (existing == null) {
            val operation = Operation().apply {
                this.code = code
                this.name = name
                this.scopeId = API_OPERATION_SCOPE_ID
            }
            saveOrThrowException { operationMapper.insert(operation) > 0 }
            saveOrThrowException { powerMapper.insert(Power().apply { id = operation.id; typeId = POWER_TYPE_OPERATION }) > 0 }
        } else {
            existing.name = name
            operationMapper.updateById(existing)
        }
    }

    private fun refreshCache() {
        apiCodeMap.clear()
        this.list().forEach { api ->
            api.code?.let { apiCodeMap[it] = api }
        }
    }
}
