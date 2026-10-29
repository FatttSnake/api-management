package top.fatweb.apimanagement.component.plugin

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute
import top.fatweb.apimanagement.service.api.IApiInterfaceService
import top.fatweb.apimanagement.service.permission.IMenuService
import top.fatweb.apimanagement.service.permission.IOperationService
import top.fatweb.apimanagement.service.permission.IPowerService
import top.fatweb.apimanagement.service.permission.IScopeService
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.updateOrThrowException

/**
 * Keeps the interface, menu, scope, operation and power rows in step with a plugin's routes.
 *
 * The routes are the source of truth: an interface row exists because a route declares it, and
 * is removed when the route disappears. Rows are keyed by the route's API code, which carries
 * the API version, so bumping the version deliberately creates a new interface.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginEndpointRegistrar(
    private val apiInterfaceService: IApiInterfaceService,
    private val menuService: IMenuService,
    private val scopeService: IScopeService,
    private val operationService: IOperationService,
    private val powerService: IPowerService,
) {
    /**
     * Registers the routes of a mounted plugin.
     *
     * @param row plugin row
     * @param routes routes collected during `onStart`
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun register(row: ApiPlugin, routes: List<RegisteredRoute>) {
        val pluginId = row.pluginId ?: return
        val menuId =
            ensureMenu(pluginId, row.name.orEmpty()).id
                ?: throw PluginInstallException("Menu of plugin $pluginId has no ID")
        val routesByCode = routes.associateBy { it.apiCode }
        val previousByCode =
            apiInterfaceService
                .list(
                    KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId),
                ).mapNotNull { interfaceRow -> interfaceRow.code?.let { code -> code to interfaceRow } }
                .toMap()

        (previousByCode.keys - routesByCode.keys).forEach { orphan ->
            previousByCode.getValue(orphan).id?.let { apiInterfaceService.removeById(it) }
        }

        // A path that moves to another route has to vacate its old value first: the unique
        // index on (path, method) would otherwise reject the new owner
        previousByCode.forEach { (code, interfaceRow) ->
            val route = routesByCode[code] ?: return@forEach
            val path = fullPath(pluginId, route)
            if (interfaceRow.path != path || interfaceRow.method != route.definition.method) {
                updateOrThrowException {
                    apiInterfaceService.update(
                        KtUpdateWrapper(ApiInterface()).apply {
                            eq(ApiInterface::id, interfaceRow.id)
                            set(ApiInterface::path, "#moving:${interfaceRow.id}")
                        },
                    )
                }
            }
        }

        val scopeIds = mutableMapOf<Int, Long>()
        routesByCode.values.forEach { route ->
            val code = route.apiCode
            val name =
                route.definition.operation.summary
                    .ifBlank { route.definition.operation.operationId }
            val path = fullPath(pluginId, route)
            val existing = previousByCode[code]
            val entity =
                existing ?: ApiInterface().apply {
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

            val scopeId =
                scopeIds.getOrPut(route.definition.apiVersion) {
                    ensureScope("v${route.definition.apiVersion}", menuId)
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

    /**
     * Removes the interface and permission rows of a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun remove(pluginId: String) {
        apiInterfaceService.remove(
            KtQueryWrapper(ApiInterface()).eq(ApiInterface::pluginId, pluginId),
        )
        val menu = menuService.getOne(KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId)) ?: return
        val menuId = menu.id ?: return
        val scopes = scopeService.list(KtQueryWrapper(Scope()).eq(Scope::menuId, menuId))
        val scopeIds = scopes.mapNotNull { it.id }
        val operations =
            if (scopeIds.isEmpty()) {
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

    private fun fullPath(pluginId: String, route: RegisteredRoute): String =
        "/api/$pluginId/v${route.definition.apiVersion}${route.definition.path}"

    private fun ensureMenu(pluginId: String, name: String): Menu {
        menuService.getOne(KtQueryWrapper(Menu()).eq(Menu::pluginId, pluginId))?.let { return it }
        val menu =
            Menu().apply {
                this.pluginId = pluginId
                this.name = name
                this.parentId = API_ROOT_MENU_ID
                this.moduleId = API_MODULE_ID
            }
        saveOrThrowException(PluginInstallException("Cannot create menu for plugin $pluginId")) {
            menuService.save(menu)
        }
        menu.id?.let { id ->
            savePower(id, POWER_TYPE_MENU)
        }
        return menu
    }

    private fun ensureScope(name: String, menuId: Long): Long {
        scopeService
            .getOne(
                KtQueryWrapper(Scope()).eq(Scope::name, name).eq(Scope::menuId, menuId),
            )?.let { scope ->
                return scope.id ?: throw PluginInstallException("Scope $name has no ID")
            }
        val scope =
            Scope().apply {
                this.name = name
                this.menuId = menuId
            }
        saveOrThrowException(PluginInstallException("Cannot create scope $name")) {
            scopeService.save(scope)
        }
        val id = scope.id ?: throw PluginInstallException("Scope $name has no ID")
        savePower(id, POWER_TYPE_SCOPE)
        return id
    }

    private fun upsertOperation(code: String, name: String, scopeId: Long) {
        val existing = operationService.getOne(KtQueryWrapper(Operation()).eq(Operation::code, code))
        if (existing == null) {
            val operation =
                Operation().apply {
                    this.code = code
                    this.name = name
                    this.scopeId = scopeId
                }
            saveOrThrowException(PluginInstallException("Cannot create operation $code")) {
                operationService.save(operation)
            }
            operation.id?.let { id ->
                savePower(id, POWER_TYPE_OPERATION)
            }
        } else {
            existing.name = name
            existing.scopeId = scopeId
            updateOrThrowException { operationService.updateById(existing) }
        }
    }

    private fun savePower(id: Long, typeId: Int) {
        saveOrThrowException {
            powerService.save(
                Power().apply {
                    this.id = id
                    this.typeId = typeId
                },
            )
        }
    }

    private companion object {
        const val API_MODULE_ID = 2000000L
        const val API_ROOT_MENU_ID = 2990000L
        const val POWER_TYPE_MENU = 2
        const val POWER_TYPE_SCOPE = 3
        const val POWER_TYPE_OPERATION = 4
    }
}
