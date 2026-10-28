package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.PluginModule
import top.fatweb.apimanagement.entity.api.PluginModuleEdge
import top.fatweb.apimanagement.entity.api.PluginModuleRef
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.mapper.api.PluginModuleMapper
import top.fatweb.apimanagement.param.system.api.PluginModuleGetParam
import top.fatweb.apimanagement.plugin.runtime.module.FileSystemPluginModuleStore
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleStore
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IPluginModuleEdgeService
import top.fatweb.apimanagement.service.api.IPluginModuleRefService
import top.fatweb.apimanagement.service.api.IPluginModuleService
import top.fatweb.apimanagement.util.PluginIdUtil
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleVo
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime

/**
 * Plugin dependency module service implement.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
@DS("master")
class PluginModuleServiceImpl(
    serverProperties: ServerProperties,
    private val pluginModuleEdgeService: IPluginModuleEdgeService,
    private val pluginModuleRefService: IPluginModuleRefService
) : ServiceImpl<PluginModuleMapper, PluginModule>(), IPluginModuleService {
    private val moduleStore: PluginModuleStore = FileSystemPluginModuleStore(
        root = Path.of(serverProperties.storage.pluginModuleDir),
        maxModuleBytes = serverProperties.storage.pluginModuleMaxBytes,
        maxModules = serverProperties.storage.pluginModuleMaxCount
    )

    /**
     * Installs every module in a lock and references them from a plugin.
     *
     * @param pluginId plugin ID
     * @param lock dependency lock
     * @param bytesOf module bytes by module
     * @return installed modules
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun installForPlugin(
        pluginId: String,
        lock: PluginModuleLock,
        bytesOf: (LockedModule) -> ByteArray?
    ): List<PluginModule> {
        requirePluginId(pluginId)
        lock.validate()

        val ordered = topologicalOrder(lock)
        val installed = LinkedHashMap<LockedModule, PluginModule>()
        ordered.forEach { module ->
            val bytes = bytesOf(module)
                ?: throw PluginInstallException("Missing dependency module: ${module.coordinate}")
            val path = moduleStore.install(module.sha256, bytes)
            if (module.size > 0 && Files.size(path) != module.size) {
                throw PluginInstallException(
                    "Module ${module.coordinate} is ${Files.size(path)} bytes, lock says ${module.size}"
                )
            }
            installed[module] = upsert(module)
        }

        ordered.forEach { module ->
            val entity = installed.getValue(module)
            val moduleId = entity.id ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId)
            )
            module.dependencies.forEach { digest ->
                val dependency = ordered.first { it.sha256 == digest }
                val dependencyId = installed.getValue(dependency).id
                    ?: throw PluginInstallException("Dependency has no ID: ${dependency.coordinate}")
                saveOrThrowException {
                    pluginModuleEdgeService.save(
                        PluginModuleEdge().apply {
                            this.moduleId = moduleId
                            this.dependencyModuleId = dependencyId
                        }
                    )
                }
            }
        }

        replaceReferences(pluginId, ordered.map { installed.getValue(it) })
        return ordered.map { installed.getValue(it) }
    }

    /**
     * Stages a module uploaded before its plugin.
     *
     * @param bytes module bytes
     * @return module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun stage(bytes: ByteArray): String {
        val digest = FileSystemPluginModuleStore.sha256(bytes)
        moduleStore.install(digest, bytes)
        return digest
    }

    /**
     * Deletes an unreferenced module.
     *
     * @param sha256 module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun delete(sha256: String) {
        val module = getOne(KtQueryWrapper(PluginModule()).eq(PluginModule::sha256, sha256))
        if (module != null) {
            val moduleId = module.id
                ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            val references = pluginModuleRefService.list(
                KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::moduleId, moduleId)
            )
            if (references.isNotEmpty()) {
                throw PluginInstallException("Module ${module.coordinate} is still referenced by a plugin")
            }
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId)
            )
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::dependencyModuleId, moduleId)
            )
            removeById(moduleId)
        }
        moduleStore.delete(sha256)
    }

    /**
     * Points a plugin at the modules of a lock that are already installed.
     *
     * @param pluginId plugin ID
     * @param lock dependency lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun reference(pluginId: String, lock: PluginModuleLock) {
        requirePluginId(pluginId)
        lock.validate()
        val modules = lock.modules.map { module ->
            get(module.group, module.name, module.version)
                ?.takeIf { it.sha256 == module.sha256 }
                ?: throw PluginInstallException("Dependency module is not installed: ${module.coordinate}")
        }
        replaceReferences(pluginId, modules)
    }

    /**
     * Removes every module reference held by a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun release(pluginId: String) {
        pluginModuleRefService.remove(
            KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::pluginId, pluginId)
        )
    }

    /**
     * Gets an installed module page.
     *
     * @param param query parameters
     * @return module page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getPage(param: PluginModuleGetParam?): PageVo<PluginModuleVo> {
        val page = Page<PluginModule>(param?.currentPage ?: 1, param?.pageSize ?: 20)
        setPageSort(param, page, OrderItem.asc("create_time"))
        val wrapper = KtQueryWrapper(PluginModule()).apply {
            param?.searchGroup?.let { like(PluginModule::groupId, it) }
            param?.searchArtifact?.let { like(PluginModule::artifactId, it) }
            param?.searchVersion?.let { like(PluginModule::moduleVersion, it) }
        }
        return page(page, wrapper).toVoPage()
    }

    /**
     * Finds one module.
     *
     * @param groupId Maven group
     * @param artifactId Maven artifact
     * @param version version
     * @return module or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun get(groupId: String, artifactId: String, version: String): PluginModule? =
        getOne(
            KtQueryWrapper(PluginModule())
                .eq(PluginModule::groupId, groupId)
                .eq(PluginModule::artifactId, artifactId)
                .eq(PluginModule::moduleVersion, version)
        )

    /**
     * Returns digests still referenced by a plugin.
     *
     * @return digests
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun referencedDigests(): Set<String> {
        val ids = pluginModuleRefService.list().mapNotNull { it.moduleId }.toSet()
        if (ids.isEmpty()) {
            return emptySet()
        }
        return listByIds(ids).mapNotNull { it.sha256 }.toSet()
    }

    /**
     * Deletes unreferenced modules past the grace period.
     *
     * @param grace retention period
     * @return number of module files deleted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Transactional
    override fun gc(grace: Duration): Int {
        val referenced = referencedDigests()
        val deleted = moduleStore.gc(referenced, grace)
        val stale = list().filter { module ->
            val digest = module.sha256
            digest != null && digest !in referenced && !moduleStore.contains(digest)
        }
        stale.forEach { module ->
            val moduleId = module.id ?: return@forEach
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId)
            )
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::dependencyModuleId, moduleId)
            )
            removeById(moduleId)
        }
        return deleted
    }

    /**
     * Returns the content-addressed module store.
     *
     * @return module store
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun store(): PluginModuleStore = moduleStore

    private fun upsert(module: LockedModule): PluginModule {
        val existing = get(module.group, module.name, module.version)
        if (existing != null) {
            if (existing.sha256 != module.sha256) {
                throw PluginInstallException(
                    "Module ${module.coordinate} is already installed with digest ${existing.sha256}"
                )
            }
            return existing
        }

        val entity = PluginModule().apply {
            groupId = module.group
            artifactId = module.name
            moduleVersion = module.version
            sha256 = module.sha256
            size = module.size
            source = module.source
            installTime = LocalDateTime.now()
        }
        saveOrThrowException(PluginInstallException("Cannot install module ${module.coordinate}")) {
            save(entity)
        }
        return entity
    }

    private fun replaceReferences(pluginId: String, modules: List<PluginModule>) {
        pluginModuleRefService.remove(
            KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::pluginId, pluginId)
        )
        modules.forEach { module ->
            val moduleId = module.id ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            saveOrThrowException {
                pluginModuleRefService.save(
                    PluginModuleRef().apply {
                        this.pluginId = pluginId
                        this.moduleId = moduleId
                    }
                )
            }
        }
    }

    private fun topologicalOrder(lock: PluginModuleLock): List<LockedModule> {
        val byDigest = lock.modules.associateBy { it.sha256 }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val result = mutableListOf<LockedModule>()

        fun visit(module: LockedModule) {
            if (module.sha256 in visited) {
                return
            }
            if (!visiting.add(module.sha256)) {
                throw PluginInstallException("Dependency cycle at ${module.coordinate}")
            }
            module.dependencies.forEach { digest ->
                visit(byDigest[digest] ?: throw PluginInstallException("Missing module $digest"))
            }
            visiting.remove(module.sha256)
            visited.add(module.sha256)
            result.add(module)
        }

        lock.modules.forEach(::visit)
        return result
    }

    private fun requirePluginId(pluginId: String) {
        if (!PluginIdUtil.REGEX.matches(pluginId)) {
            throw PluginInstallException("Invalid plugin ID: $pluginId")
        }
    }
}
