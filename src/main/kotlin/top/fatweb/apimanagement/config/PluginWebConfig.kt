package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.ApiPrincipal
import top.fatweb.apimanagement.api.v1.InterfaceInfo
import top.fatweb.apimanagement.api.v1.PrincipalType
import top.fatweb.apimanagement.aspectj.ApiAccessInterceptor
import top.fatweb.apimanagement.aspectj.SysLogInterceptor
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.plugin.runtime.route.PluginApiHandlerAdapter
import top.fatweb.apimanagement.plugin.runtime.route.PluginApiHandlerMapping
import top.fatweb.apimanagement.plugin.runtime.route.PluginRouteRegistry

/**
 * Spring MVC integration for plugin routes.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class PluginWebConfig(
    private val routeRegistry: PluginRouteRegistry,
    private val apiAccessInterceptor: ApiAccessInterceptor,
    private val sysLogInterceptor: SysLogInterceptor,
    private val jsonMapper: JsonMapper,
    private val registryCache: PluginRegistryCache,
) {
    /**
     * Handler mapping for `/api/{pluginId}/v{n}/...`.
     *
     * @return handler mapping
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginApiHandlerMapping(): PluginApiHandlerMapping = PluginApiHandlerMapping(
        registry = routeRegistry,
        interceptors = listOf(sysLogInterceptor, apiAccessInterceptor),
    )

    /**
     * Handler adapter that invokes plugin handlers.
     *
     * @return handler adapter
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginApiHandlerAdapter(): PluginApiHandlerAdapter = PluginApiHandlerAdapter(
        jsonMapper = jsonMapper,
        principalResolver = {
            when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
                is ApiKeyPrincipal ->
                    ApiPrincipal(
                        type = PrincipalType.ACCESS_KEY,
                        userId = principal.userId,
                        accessKeyId = principal.keyId,
                        name = "",
                    )

                is LoginUser ->
                    ApiPrincipal(
                        type = PrincipalType.USER,
                        userId = principal.user.id,
                        accessKeyId = null,
                        name = principal.user.username.orEmpty(),
                    )

                else -> null
            }
        },
        interfaceResolver = { invocation ->
            val row = registryCache.getByCode(invocation.apiCode)
            if (row == null) {
                InterfaceInfo(
                    code = invocation.apiCode,
                    pluginId = invocation.pluginId,
                    apiVersion = invocation.definition.apiVersion,
                    name = invocation.definition.operation.summary,
                    path = invocation.definition.path,
                    method = invocation.definition.method,
                )
            } else {
                InterfaceInfo(
                    code = row.code ?: invocation.apiCode,
                    pluginId = row.pluginId ?: invocation.pluginId,
                    apiVersion = row.apiVersion ?: invocation.definition.apiVersion,
                    name = row.name.orEmpty(),
                    enabled = row.enable == 1,
                    price = row.price,
                    billingMode = row.billingMode?.code.orEmpty(),
                    needKey = row.needKey == 1,
                    rateLimit = row.rateLimit,
                    accessMode = row.accessMode?.code.orEmpty(),
                    path = row.path.orEmpty(),
                    method = row.method.orEmpty(),
                )
            }
        },
    )
}
