package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.converter.api.toVoPage
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.api.PluginModule
import top.fatweb.apimanagement.entity.api.PluginModuleEdge
import top.fatweb.apimanagement.entity.api.PluginModuleRef
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
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
import top.fatweb.apimanagement.util.MavenCoordinate
import top.fatweb.apimanagement.util.MavenCoordinateUtil
import top.fatweb.apimanagement.util.PluginIdUtil
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleReferenceVo
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
    private val pluginModuleRefService: IPluginModuleRefService,
    private val apiPluginMapper: ApiPluginMapper,
) : ServiceImpl<PluginModuleMapper, PluginModule>(),
    IPluginModuleService {
    private val maxModules: Int = serverProperties.storage.pluginModuleMaxCount
    private val moduleStore: PluginModuleStore =
        FileSystemPluginModuleStore(
            root = Path.of(serverProperties.storage.pluginModuleDir),
            maxModuleBytes = serverProperties.storage.pluginModuleMaxBytes,
            maxModules = maxModules,
        )

    /**
     * Claims every module in a lock from the module store and references them from a plugin.
     *
     * @param pluginId plugin ID
     * @param lock dependency lock
     * @return installed modules
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    @Transactional
    override fun installForPlugin(pluginId: String, lock: PluginModuleLock): List<PluginModule> {
        requirePluginId(pluginId)
        lock.validate(maxModules)

        val ordered = topologicalOrder(lock)
        val installed = LinkedHashMap<LockedModule, PluginModule>()
        ordered.forEach { module ->
            val path = moduleStore.path(module.sha256)
            if (!moduleStore.contains(module.sha256)) {
                throw PluginInstallException("Missing dependency module: ${module.coordinate}")
            }
            if (!moduleStore.verify(module.sha256)) {
                throw PluginInstallException("Dependency module ${module.coordinate} is corrupted")
            }
            if (module.size > 0 && Files.size(path) != module.size) {
                throw PluginInstallException(
                    "Module ${module.coordinate} is ${Files.size(path)} bytes, lock says ${module.size}",
                )
            }
            installed[module] = upsert(module)
        }

        ordered.forEach { module ->
            val entity = installed.getValue(module)
            val moduleId = entity.id ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId),
            )
            module.dependencies.forEach { digest ->
                val dependency = ordered.first { it.sha256 == digest }
                val dependencyId =
                    installed.getValue(dependency).id
                        ?: throw PluginInstallException("Dependency has no ID: ${dependency.coordinate}")
                saveOrThrowException {
                    pluginModuleEdgeService.save(
                        PluginModuleEdge().apply {
                            this.moduleId = moduleId
                            this.dependencyModuleId = dependencyId
                        },
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
     * The row is what makes the upload visible in the list. Its coordinate is read from the
     * jar's Maven metadata; a jar without it stays coordinate-less until a plugin's lock
     * claims the digest.
     *
     * @param bytes module bytes
     * @param source repository the module was downloaded from, when known
     * @param coordinate coordinate the plugin lock declares, when known
     * @return module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    @Transactional
    override fun stage(bytes: ByteArray, source: String?, coordinate: MavenCoordinate?): String {
        val digest = FileSystemPluginModuleStore.sha256(bytes)
        moduleStore.install(digest, bytes)
        val resolved = coordinate ?: MavenCoordinateUtil.read(bytes)
        val staged = getOne(KtQueryWrapper(PluginModule()).eq(PluginModule::sha256, digest))
        if (staged == null) {
            saveOrThrowException(PluginInstallException("Cannot stage module $digest")) {
                save(
                    PluginModule().apply {
                        groupId = resolved?.groupId
                        artifactId = resolved?.artifactId
                        moduleVersion = resolved?.version
                        sha256 = digest
                        size = bytes.size.toLong()
                        this.source = source
                        installTime = LocalDateTime.now()
                    },
                )
            }
            return digest
        }

        var changed = false
        if (source != null && staged.source == null) {
            staged.source = source
            changed = true
        }
        val current = listOfNotNull(staged.groupId, staged.artifactId, staged.moduleVersion)
        val target = coordinate ?: resolved?.takeIf { current.isEmpty() }
        if (target != null && current != listOf(target.groupId, target.artifactId, target.version)) {
            staged.groupId = target.groupId
            staged.artifactId = target.artifactId
            staged.moduleVersion = target.version
            changed = true
        }
        if (changed) {
            saveOrThrowException(PluginInstallException("Cannot stage module $digest")) {
                updateById(staged)
            }
        }
        return digest
    }

    /**
     * Deletes an unreferenced module.
     *
     * @param sha256 module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    @Transactional
    override fun delete(sha256: String) {
        val module = getOne(KtQueryWrapper(PluginModule()).eq(PluginModule::sha256, sha256))
        if (module != null) {
            val moduleId =
                module.id
                    ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            val references =
                pluginModuleRefService.list(
                    KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::moduleId, moduleId),
                )
            if (references.isNotEmpty()) {
                val pluginIds = references.mapNotNull { it.pluginId }.sorted().joinToString()
                throw PluginInstallException("Module ${module.coordinate} is still referenced by $pluginIds")
            }
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId),
            )
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::dependencyModuleId, moduleId),
            )
            removeById(moduleId)
        }
        moduleStore.delete(sha256)
    }

    /**
     * Removes every module reference held by a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    @Transactional
    override fun release(pluginId: String) {
        pluginModuleRefService.remove(
            KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::pluginId, pluginId),
        )
    }

    /**
     * Gets an uploaded module page, including the plugins referencing each module.
     *
     * @param param query parameters
     * @return module page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getPage(param: PluginModuleGetParam?): PageVo<PluginModuleVo> {
        val page = Page<PluginModule>(param?.currentPage ?: 1, param?.pageSize ?: 20)
        setPageSort(param, page, OrderItem.asc("create_time"))
        val wrapper =
            KtQueryWrapper(PluginModule()).apply {
                param?.searchGroup?.let { like(PluginModule::groupId, it) }
                param?.searchArtifact?.let { like(PluginModule::artifactId, it) }
                param?.searchVersion?.let { like(PluginModule::moduleVersion, it) }
                param?.searchSha?.let { likeRight(PluginModule::sha256, it) }
                when (param?.referenced) {
                    true -> inSql(PluginModule::id, REFERENCED_MODULE_IDS)
                    false -> notInSql(PluginModule::id, REFERENCED_MODULE_IDS)
                    null -> {}
                }
            }
        val modulePage = page(page, wrapper)
        return modulePage.toVoPage(referencesByModule(modulePage.records.mapNotNull { it.id }))
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
    override fun get(groupId: String, artifactId: String, version: String): PluginModule? = getOne(
        KtQueryWrapper(PluginModule())
            .eq(PluginModule::groupId, groupId)
            .eq(PluginModule::artifactId, artifactId)
            .eq(PluginModule::moduleVersion, version),
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
    @Synchronized
    @Transactional
    override fun gc(grace: Duration): Int {
        val referenced = referencedDigests()
        val deleted = moduleStore.gc(referenced, grace)
        val stale =
            list().filter { module ->
                val digest = module.sha256
                digest != null && digest !in referenced && !moduleStore.contains(digest)
            }
        stale.forEach { module ->
            val moduleId = module.id ?: return@forEach
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::moduleId, moduleId),
            )
            pluginModuleEdgeService.remove(
                KtQueryWrapper(PluginModuleEdge()).eq(PluginModuleEdge::dependencyModuleId, moduleId),
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
        val staged = getOne(KtQueryWrapper(PluginModule()).eq(PluginModule::sha256, module.sha256))
        if (staged != null) {
            claim(module, staged)
            return staged
        }

        val existing = get(module.group, module.name, module.version)
        if (existing != null) {
            throw PluginInstallException(
                "Module ${module.coordinate} is already installed with digest ${existing.sha256}",
            )
        }

        val entity =
            PluginModule().apply {
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

    /**
     * Fills a staged module's details from the lock that claims it.
     *
     * The lock lives inside the signed plugin jar and wins over the jar's own metadata: a
     * shaded jar can carry the coordinate of the artifact it bundled.
     *
     * @param module module declared by the lock
     * @param staged row the upload created
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun claim(module: LockedModule, staged: PluginModule) {
        val owner = get(module.group, module.name, module.version)
        if (owner != null && owner.id != staged.id) {
            throw PluginInstallException(
                "Module ${module.coordinate} is already installed with digest ${owner.sha256}",
            )
        }

        val coordinateChanged =
            staged.groupId != module.group ||
                staged.artifactId != module.name ||
                staged.moduleVersion != module.version
        val sourceMissing = staged.source == null && module.source != null
        val sizeMissing = (staged.size ?: 0) <= 0 && module.size > 0
        if (!coordinateChanged && !sourceMissing && !sizeMissing) {
            return
        }

        staged.groupId = module.group
        staged.artifactId = module.name
        staged.moduleVersion = module.version
        if (sourceMissing) {
            staged.source = module.source
        }
        if (sizeMissing) {
            staged.size = module.size
        }
        saveOrThrowException(PluginInstallException("Cannot register module ${module.coordinate}")) {
            updateById(staged)
        }
    }

    /**
     * Resolves the plugins that reference each module of one page.
     *
     * @param moduleIds module IDs on the current page
     * @return references by module ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun referencesByModule(moduleIds: List<Long>): Map<Long, List<PluginModuleReferenceVo>> {
        if (moduleIds.isEmpty()) {
            return emptyMap()
        }
        val references =
            pluginModuleRefService.list(
                KtQueryWrapper(PluginModuleRef()).`in`(PluginModuleRef::moduleId, moduleIds),
            )
        val pluginIds = references.mapNotNull { it.pluginId }.distinct()
        val plugins =
            if (pluginIds.isEmpty()) {
                emptyMap()
            } else {
                apiPluginMapper
                    .selectList(KtQueryWrapper(ApiPlugin()).`in`(ApiPlugin::pluginId, pluginIds))
                    .mapNotNull { plugin -> plugin.pluginId?.let { it to plugin } }
                    .toMap()
            }

        val byModule = mutableMapOf<Long, MutableList<PluginModuleReferenceVo>>()
        references.forEach { reference ->
            val moduleId = reference.moduleId ?: return@forEach
            val plugin = plugins[reference.pluginId]
            byModule
                .getOrPut(moduleId) { mutableListOf() }
                .add(
                    PluginModuleReferenceVo(
                        pluginId = reference.pluginId,
                        name = plugin?.name ?: reference.pluginId,
                        enable = plugin?.let { it.enable == 1 },
                    ),
                )
        }
        return byModule.mapValues { (_, moduleReferences) -> moduleReferences.sortedBy { it.pluginId } }
    }

    private fun replaceReferences(pluginId: String, modules: List<PluginModule>) {
        pluginModuleRefService.remove(
            KtQueryWrapper(PluginModuleRef()).eq(PluginModuleRef::pluginId, pluginId),
        )
        modules.forEach { module ->
            val moduleId = module.id ?: throw PluginInstallException("Module has no ID: ${module.coordinate}")
            saveOrThrowException {
                pluginModuleRefService.save(
                    PluginModuleRef().apply {
                        this.pluginId = pluginId
                        this.moduleId = moduleId
                    },
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

private const val REFERENCED_MODULE_IDS = "select module_id from t_b_plugin_module_ref"
