package top.fatweb.apimanagement.plugin.runtime.classloader

import top.fatweb.apimanagement.plugin.runtime.module.LoaderUnit
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLockException
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleStore
import top.fatweb.apimanagement.plugin.runtime.module.loaderUnits
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gateway-wide registry of shared loader units.
 *
 * Units with the same member set and external dependency-unit fingerprints share one loader
 * and one copy of their classes. Distinct versions of the same artifact coexist because their
 * keys differ.
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

        val units = lock.loaderUnits()
        synchronized(monitor) {
            check(!closed) { "Module registry is closed" }

            val keyByUnit = HashMap<Int, ModuleKey>()
            val loaderByUnit = HashMap<Int, ModuleClassLoader>()
            val loaderByDigest = LinkedHashMap<String, ModuleClassLoader>()
            val acquired = LinkedHashSet<ModuleKey>()

            try {
                units.forEach { unit ->
                    val dependencyFingerprints =
                        unit.dependencies
                            .map { id ->
                                keyByUnit[id]?.fingerprint()
                                    ?: throw PluginModuleLockException(
                                        "${unitLabel(unit)} depends on an unresolved loader unit $id",
                                    )
                            }.sorted()
                    val key =
                        ModuleKey(
                            members = unit.members.map { ModuleMember(it.coordinate, it.sha256) },
                            dependencies = dependencyFingerprints,
                        )
                    keyByUnit[unit.id] = key

                    val existing = shared[key]
                    val loader =
                        if (existing != null) {
                            if (acquired.add(key)) {
                                existing.references++
                            }
                            existing.loader
                        } else {
                            val dependencyLoaders =
                                unit.dependencies.map { id ->
                                    loaderByUnit[id]
                                        ?: throw PluginModuleLockException(
                                            "${unitLabel(unit)} depends on an unloaded loader unit $id",
                                        )
                                }
                            val created =
                                ModuleClassLoader(
                                    key,
                                    unit.members.map { store.path(it.sha256) },
                                    platform,
                                    dependencyLoaders,
                                )
                            shared[key] = SharedModule(created)
                            acquired.add(key)
                            created
                        }
                    loaderByUnit[unit.id] = loader
                    unit.members.forEach { loaderByDigest[it.sha256] = loader }
                }
            } catch (ex: Throwable) {
                releaseLocked(acquired)
                throw ex
            }

            return PluginModuleGraph(
                loaderByDigest = loaderByDigest,
                loaders = units.map { loaderByUnit.getValue(it.id) }.distinct(),
                keys = acquired.toList(),
                registry = this,
            )
        }
    }

    /**
     * Returns the number of shared loader units. Intended for diagnostics and tests.
     *
     * @return shared loader count
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sharedLoaderCount(): Int = synchronized(monitor) { shared.size }

    /**
     * Returns the reference count of a shared loader unit. Intended for diagnostics and tests.
     *
     * @param key unit key
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

    private fun unitLabel(unit: LoaderUnit): String =
        unit.members.joinToString(prefix = "[", postfix = "]") { it.coordinate }

    private class SharedModule(val loader: ModuleClassLoader) {
        var references: Int = 1
    }
}

/**
 * A plugin's acquired module graph.
 *
 * @property loaders unit loaders in dependency-first order
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleGraph internal constructor(
    private val loaderByDigest: Map<String, ModuleClassLoader>,
    private val loaders: List<ModuleClassLoader>,
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
    fun loaderFor(sha256: String): ModuleClassLoader? = loaderByDigest[sha256]

    /**
     * Returns all unit loaders, without duplicates.
     *
     * @return unit loaders
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loaders(): Collection<ModuleClassLoader> = loaders

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
