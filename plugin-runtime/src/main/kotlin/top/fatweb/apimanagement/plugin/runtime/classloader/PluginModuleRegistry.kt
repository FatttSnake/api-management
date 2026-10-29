package top.fatweb.apimanagement.plugin.runtime.classloader

import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLockException
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleStore
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gateway-wide registry of shared module class loaders.
 *
 * Modules with the same coordinate, digest and direct dependency set share one loader and one
 * copy of their classes. Distinct versions of the same artifact coexist because their keys
 * differ.
 *
 * @param store module store
 * @param platform platform class loader
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleRegistry(private val store: PluginModuleStore, private val platform: ClassLoader) : AutoCloseable {
    private val shared = LinkedHashMap<ModuleKey, SharedModule>()
    private val monitor = Any()
    private var closed = false

    /**
     * Acquires loaders for every module in a lock.
     *
     * @param lock plugin dependency lock
     * @return acquired module graph
     * @throws PluginModuleMissingException when a module is not installed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun acquire(lock: PluginModuleLock): PluginModuleGraph {
        lock.validate()
        val missing =
            lock.modules
                .filterNot { store.contains(it.sha256) }
                .map { it.coordinate }
        if (missing.isNotEmpty()) {
            throw PluginModuleMissingException(missing)
        }

        val ordered = topologicalOrder(lock)
        synchronized(monitor) {
            check(!closed) { "Module registry is closed" }

            val keyByDigest = mutableMapOf<String, ModuleKey>()
            val loaderByDigest = mutableMapOf<String, ModuleClassLoader>()
            val acquired = LinkedHashSet<ModuleKey>()

            try {
                ordered.forEach { module ->
                    val dependencyFingerprints =
                        module.dependencies
                            .map { digest ->
                                keyByDigest[digest]?.fingerprint()
                                    ?: throw PluginModuleLockException(
                                        "${module.coordinate} depends on an unresolved module $digest",
                                    )
                            }.sorted()
                    val key = ModuleKey(module.coordinate, module.sha256, dependencyFingerprints)
                    keyByDigest[module.sha256] = key

                    val existing = shared[key]
                    val loader =
                        if (existing != null) {
                            if (acquired.add(key)) {
                                existing.references++
                            }
                            existing.loader
                        } else {
                            val dependencyLoaders =
                                module.dependencies.map { digest ->
                                    loaderByDigest[digest]
                                        ?: throw PluginModuleLockException(
                                            "${module.coordinate} depends on an unloaded module $digest",
                                        )
                                }
                            val created =
                                ModuleClassLoader(
                                    key,
                                    store.path(module.sha256),
                                    platform,
                                    dependencyLoaders,
                                )
                            shared[key] = SharedModule(created)
                            acquired.add(key)
                            created
                        }
                    loaderByDigest[module.sha256] = loader
                }
            } catch (ex: Throwable) {
                releaseLocked(acquired)
                throw ex
            }

            return PluginModuleGraph(loaderByDigest.toMap(), acquired.toList(), this)
        }
    }

    /**
     * Returns the number of shared loaders. Intended for diagnostics and tests.
     *
     * @return shared loader count
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sharedLoaderCount(): Int = synchronized(monitor) { shared.size }

    /**
     * Returns the reference count of a shared loader. Intended for diagnostics and tests.
     *
     * @param key module key
     * @return reference count, or 0 when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun references(key: ModuleKey): Int = synchronized(monitor) { shared[key]?.references ?: 0 }

    internal fun release(keys: Collection<ModuleKey>) {
        synchronized(monitor) {
            releaseLocked(keys)
        }
    }

    /**
     * Closes every shared loader.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close() {
        synchronized(monitor) {
            if (closed) {
                return
            }
            closed = true
            shared.values.forEach { it.loader.close() }
            shared.clear()
        }
    }

    private fun releaseLocked(keys: Collection<ModuleKey>) {
        keys.forEach { key ->
            val module = shared[key] ?: return@forEach
            module.references--
            if (module.references <= 0) {
                shared.remove(key)
                module.loader.close()
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
                throw PluginModuleLockException("Dependency cycle at ${module.coordinate}")
            }
            module.dependencies.forEach { digest ->
                visit(byDigest[digest] ?: throw PluginModuleLockException("Missing module $digest"))
            }
            visiting.remove(module.sha256)
            visited.add(module.sha256)
            result.add(module)
        }

        lock.modules.forEach(::visit)
        return result
    }

    private class SharedModule(val loader: ModuleClassLoader) {
        var references: Int = 1
    }
}

/**
 * A plugin's acquired module graph.
 *
 * @property loadersByDigest module loaders by module digest
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleGraph internal constructor(
    val loadersByDigest: Map<String, ModuleClassLoader>,
    private val keys: List<ModuleKey>,
    private val registry: PluginModuleRegistry,
) : AutoCloseable {
    private val released = AtomicBoolean(false)

    /**
     * Returns the loader for a module digest.
     *
     * @param sha256 module digest
     * @return loader, or null when the digest is not in this graph
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loaderFor(sha256: String): ModuleClassLoader? = loadersByDigest[sha256]

    /**
     * Returns all module loaders.
     *
     * @return module loaders
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loaders(): Collection<ModuleClassLoader> = loadersByDigest.values

    /**
     * Returns the module keys acquired by this graph.
     *
     * @return module keys
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun moduleKeys(): List<ModuleKey> = keys

    /**
     * Releases this graph's references.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close() {
        if (released.compareAndSet(false, true)) {
            registry.release(keys)
        }
    }
}

/**
 * A lock references a module that is not installed.
 *
 * @property missing missing coordinates
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleMissingException(val missing: List<String>) :
    RuntimeException("Missing plugin modules: ${missing.joinToString(", ")}")
