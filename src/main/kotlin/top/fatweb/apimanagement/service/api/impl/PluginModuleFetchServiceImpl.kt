package top.fatweb.apimanagement.service.api.impl

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.component.plugin.PluginArtifactStore
import top.fatweb.apimanagement.component.plugin.PluginModuleDownloader
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IPluginModuleFetchService
import top.fatweb.apimanagement.service.api.IPluginModuleService
import top.fatweb.apimanagement.util.MavenCoordinate
import top.fatweb.apimanagement.vo.api.PluginModuleFetchModuleStatus
import top.fatweb.apimanagement.vo.api.PluginModuleFetchModuleVo
import top.fatweb.apimanagement.vo.api.PluginModuleFetchStatus
import top.fatweb.apimanagement.vo.api.PluginModuleFetchVo
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * Plugin dependency module fetch service implement.
 *
 * @param artifactStore plugin artifact store
 * @param moduleService module service
 * @param downloader module downloader
 * @param executor executor running fetch tasks
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
class PluginModuleFetchServiceImpl(
    private val artifactStore: PluginArtifactStore,
    private val moduleService: IPluginModuleService,
    private val downloader: PluginModuleDownloader,
    serverProperties: ServerProperties,
    @param:Qualifier("pluginModuleFetchExecutor") private val executor: Executor,
) : IPluginModuleFetchService {
    private val logger = LoggerFactory.getLogger(PluginModuleFetchServiceImpl::class.java)
    private val maxModules = serverProperties.storage.pluginModuleMaxCount
    private val maxJarBytes = serverProperties.storage.pluginJarMaxBytes
    private val tasks = ConcurrentHashMap<String, FetchTask>()

    /**
     * Starts downloading the modules of a plugin jar that are not in the module store.
     *
     * @param jarBytes plugin jar
     * @return task snapshot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun start(jarBytes: ByteArray): PluginModuleFetchVo {
        if (jarBytes.size > maxJarBytes) {
            throw PluginInstallException(
                "Plugin jar is ${jarBytes.size} bytes, limit is $maxJarBytes",
            )
        }
        artifactStore.descriptor(jarBytes)
        val lock = artifactStore.lock(jarBytes)
        try {
            lock.validate(maxModules)
        } catch (ex: Exception) {
            throw PluginInstallException("Invalid dependency lock: ${ex.message}", ex)
        }
        val task = FetchTask(UUID.randomUUID().toString(), lock.modules)
        task.modules.forEach { module ->
            if (available(module.module.sha256)) {
                module.status = PluginModuleFetchModuleStatus.EXISTING
                module.downloadedBytes = module.module.size
            }
        }
        val pending = task.modules.filter { it.status == PluginModuleFetchModuleStatus.PENDING }
        if (pending.isNotEmpty() && !downloader.online) {
            throw PluginInstallException(
                "Missing dependency modules: ${pending.joinToString { it.module.coordinate }}; " +
                    "online module download is disabled",
            )
        }

        tasks[task.id] = task
        prune()
        if (pending.isEmpty()) {
            task.complete(PluginModuleFetchStatus.SUCCESS, null)
        } else {
            executor.execute { run(task) }
        }
        return task.toVo()
    }

    /**
     * Gets a task snapshot.
     *
     * @param taskId task ID
     * @return task snapshot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun get(taskId: String): PluginModuleFetchVo = tasks[taskId]?.toVo() ?: throw NoRecordFoundException()

    /**
     * Downloads every missing module of a lock before an install.
     *
     * @param lock dependency lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun ensure(lock: PluginModuleLock) {
        val missing = lock.modules.filterNot { available(it.sha256) }
        if (missing.isEmpty()) {
            return
        }
        if (!downloader.online) {
            throw PluginInstallException(
                "Missing dependency modules: ${missing.joinToString { it.coordinate }}; " +
                    "online module download is disabled",
            )
        }
        missing.forEach { module ->
            val downloaded = downloader.download(module)
            moduleService.stage(
                downloaded.bytes,
                downloaded.source,
                MavenCoordinate(module.group, module.name, module.version),
            )
        }
    }

    private fun run(task: FetchTask) {
        try {
            task.modules
                .filter { it.status == PluginModuleFetchModuleStatus.PENDING }
                .forEach { module -> download(task, module) }
            task.complete(PluginModuleFetchStatus.SUCCESS, null)
        } catch (ex: Exception) {
            logger.warn("Cannot fetch dependency modules of a plugin", ex)
            task.complete(PluginModuleFetchStatus.FAILED, task.message ?: ex.message)
        }
    }

    private fun download(task: FetchTask, module: FetchModule) {
        module.status = PluginModuleFetchModuleStatus.DOWNLOADING
        try {
            val downloaded =
                downloader.download(module.module) { bytes ->
                    module.downloadedBytes = bytes
                }
            module.downloadedBytes = downloaded.bytes.size.toLong()
            moduleService.stage(
                downloaded.bytes,
                downloaded.source,
                MavenCoordinate(module.module.group, module.module.name, module.module.version),
            )
            module.status = PluginModuleFetchModuleStatus.INSTALLED
        } catch (ex: Exception) {
            module.status = PluginModuleFetchModuleStatus.FAILED
            module.message = ex.message ?: ex.javaClass.simpleName
            task.message = "Cannot fetch ${module.module.coordinate}: ${module.message}"
            throw ex
        }
    }

    private fun available(sha256: String): Boolean {
        val store = moduleService.store()
        if (!store.contains(sha256)) {
            return false
        }
        if (store.verify(sha256)) {
            return true
        }
        runCatching { store.delete(sha256) }
        return false
    }

    private fun prune() {
        val cutoff = LocalDateTime.now().minusHours(TASK_RETENTION_HOURS)
        tasks.entries.removeIf { (_, task) ->
            task.finishTime?.isBefore(cutoff) == true
        }
    }

    /**
     * One fetch task.
     *
     * @param id task ID
     * @param modules locked modules
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private class FetchTask(id: String, modules: List<LockedModule>) {
        val id: String = id
        val modules: List<FetchModule> = modules.map(::FetchModule)
        val createTime: LocalDateTime = LocalDateTime.now()

        @Volatile
        var status: PluginModuleFetchStatus = PluginModuleFetchStatus.RUNNING

        @Volatile
        var message: String? = null

        @Volatile
        var finishTime: LocalDateTime? = null

        fun complete(status: PluginModuleFetchStatus, message: String?) {
            this.status = status
            this.message = message ?: this.message
            this.finishTime = LocalDateTime.now()
        }

        fun toVo(): PluginModuleFetchVo = PluginModuleFetchVo(
            taskId = id,
            status = status,
            message = message,
            modules = modules.map { it.toVo() },
            createTime = createTime,
            finishTime = finishTime,
        )
    }

    /**
     * One module of a fetch task.
     *
     * @param module locked module
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private class FetchModule(val module: LockedModule) {
        @Volatile
        var status: PluginModuleFetchModuleStatus = PluginModuleFetchModuleStatus.PENDING

        @Volatile
        var downloadedBytes: Long = 0

        @Volatile
        var message: String? = null

        fun toVo(): PluginModuleFetchModuleVo = PluginModuleFetchModuleVo(
            groupId = module.group,
            artifactId = module.name,
            moduleVersion = module.version,
            sha256 = module.sha256,
            size = module.size,
            downloadedBytes = downloadedBytes,
            percent = percent(),
            status = status,
            message = message,
        )

        private fun percent(): Int = when {
            status == PluginModuleFetchModuleStatus.INSTALLED ||
                status == PluginModuleFetchModuleStatus.EXISTING -> 100

            module.size > 0 -> (downloadedBytes * 100 / module.size).coerceIn(0, 100).toInt()
            else -> 0
        }
    }

    private companion object {
        const val TASK_RETENTION_HOURS = 1L
    }
}
