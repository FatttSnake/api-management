package top.fatweb.apimanagement.component.plugin.runtime

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.plugin.PluginArtifactStore
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaCache
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
import top.fatweb.apimanagement.component.plugin.PluginInFlightTracker
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.exception.PluginPurgeFailedException
import top.fatweb.apimanagement.plugin.runtime.route.PluginRouteRegistry
import top.fatweb.apimanagement.plugin.runtime.runtime.LoadedPlugin
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginRuntimeLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Owns the plugin runtimes and serialises their lifecycle.
 *
 * A plugin is mounted by [load] and made live by [activate]; [unload] takes it down in the
 * order the plugin contract promises (routes, subscriptions, timers, in-flight requests, hooks,
 * class loader). One lock per plugin keeps two administrative operations from interleaving.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginRuntimeRegistry(
    private val artifactStore: PluginArtifactStore,
    private val runtimeLoader: PluginRuntimeLoader,
    private val capabilityFactory: PluginCapabilityFactory,
    private val routeRegistry: PluginRouteRegistry,
    private val eventBus: PluginEventBus,
    private val scheduler: PluginScheduler,
    private val inFlightTracker: PluginInFlightTracker,
    private val configSchemaCache: PluginConfigSchemaCache,
) {
    private val logger = LoggerFactory.getLogger(PluginRuntimeRegistry::class.java)
    private val runtimes = ConcurrentHashMap<String, LoadedPlugin>()
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * Whether a plugin has a live runtime.
     *
     * @param pluginId plugin ID
     * @return true when mounted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isRunning(pluginId: String): Boolean = runtimes.containsKey(pluginId)

    /**
     * Loads and starts a plugin from its stored jar.
     *
     * The runtime is not reachable by requests until [activate] is called.
     *
     * @param row plugin row
     * @return mounted runtime
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun load(row: ApiPlugin): LoadedPlugin {
        val pluginId = row.pluginId ?: throw PluginInstallException("Plugin row has no pluginId")
        val hash = row.fileHash ?: throw PluginInstallException("Plugin $pluginId has no stored jar")
        val path = artifactStore.path(hash)
        if (!Files.isRegularFile(path)) {
            throw PluginInstallException("Plugin jar is missing: $path")
        }
        return load(pluginId, path, PluginConfigSchemaUtil.parseOrThrow(row.configSchema))
    }

    /**
     * Loads a jar that has not been recorded in a row yet.
     *
     * @param pluginId plugin ID
     * @param jar plugin jar
     * @param schema schema the jar declares
     * @return mounted runtime
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun load(pluginId: String, jar: Path, schema: PluginConfigSchema?): LoadedPlugin {
        configSchemaCache.put(pluginId, schema)
        return runtimeLoader.load(jar, capabilityFactory.create(pluginId, schema))
    }

    /**
     * Makes a mounted runtime live by publishing its routes.
     *
     * @param pluginId plugin ID
     * @param loaded mounted runtime
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun activate(pluginId: String, loaded: LoadedPlugin) {
        runtimes[pluginId] = loaded
        routeRegistry.replace(pluginId, loaded.routes)
    }

    /**
     * Takes a plugin runtime down and releases what it owns.
     *
     * @param pluginId plugin ID
     * @param purge whether to run `onPurge`
     * @param uninstalling whether to run `onUninstall`
     * @throws PluginPurgeFailedException when `purge` is requested and `onPurge` fails
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun unload(pluginId: String, purge: Boolean, uninstalling: Boolean) {
        inFlightTracker.beginDrain(pluginId)
        routeRegistry.unregisterPlugin(pluginId)
        eventBus.removePlugin(pluginId)
        scheduler.removePlugin(pluginId)
        if (!inFlightTracker.awaitDrain(pluginId, IN_FLIGHT_DRAIN_TIMEOUT_MILLIS)) {
            logger.warn(
                "Plugin {} still has requests in flight after {} ms; closing its runtime anyway",
                pluginId,
                IN_FLIGHT_DRAIN_TIMEOUT_MILLIS,
            )
        }

        val loaded = runtimes.remove(pluginId)
        var purgeFailure: Throwable? = null
        if (loaded != null) {
            if (purge) {
                try {
                    loaded.purge()
                } catch (ex: Throwable) {
                    purgeFailure = ex
                    logger.error("Plugin {} onPurge failed", pluginId, ex)
                }
            }
            runCatching { loaded.stop() }
                .onFailure { logger.error("Plugin {} onStop failed", pluginId, it) }
            if (uninstalling) {
                runCatching { loaded.uninstall() }
                    .onFailure { logger.error("Plugin {} onUninstall failed", pluginId, it) }
            }
            runCatching { loaded.close() }
                .onFailure { logger.error("Plugin {} resources failed to close", pluginId, it) }
        }
        inFlightTracker.release(pluginId)

        purgeFailure?.let {
            throw PluginPurgeFailedException(
                "Plugin $pluginId failed to purge its data: ${it.message}",
                it,
            )
        }
    }

    /**
     * Stops and closes a runtime that started but never became active.
     *
     * @param pluginId plugin ID
     * @param loaded mounted runtime
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun discard(pluginId: String, loaded: LoadedPlugin) {
        runtimes.remove(pluginId, loaded)
        eventBus.removePlugin(pluginId)
        scheduler.removePlugin(pluginId)
        runCatching { loaded.stop() }
            .onFailure { logger.warn("Plugin {} onStop failed while discarding its runtime", pluginId, it) }
        runCatching { loaded.close() }
            .onFailure { logger.warn("Plugin {} resources failed to close", pluginId, it) }
    }

    /**
     * Runs an operation while holding the lifecycle lock of one plugin.
     *
     * @param pluginId plugin ID
     * @param action operation
     * @return operation result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> withLock(pluginId: String, action: () -> T): T =
        locks.computeIfAbsent(pluginId) { ReentrantLock() }.withLock(action)

    private companion object {
        const val IN_FLIGHT_DRAIN_TIMEOUT_MILLIS = 10_000L
    }
}
