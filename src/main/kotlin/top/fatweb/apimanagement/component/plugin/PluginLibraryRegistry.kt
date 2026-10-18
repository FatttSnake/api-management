package top.fatweb.apimanagement.component.plugin

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.sdk.plugin.PluginLibraryIndex
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Gateway-wide registry of shared plugin dependency class loaders
 *
 * A library is keyed by its digest and direct-dependency digests, so two plugins that bundle
 * the same jar share one class loader and one set of loaded classes. Reference counting keeps
 * the loader open until the last plugin using it is unmounted.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginLibraryRegistry {
    private val lock = ReentrantLock()
    private val shared = mutableMapOf<Key, SharedLibrary>()
    private val gateway: ClassLoader = PluginLibraryRegistry::class.java.classLoader

    /**
     * Acquire the libraries of one plugin mount
     *
     * @param installed Libraries materialized from the plugin jar
     * @return Reference-counted library set
     * @throws IllegalStateException when the index cannot be ordered
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun acquire(installed: InstalledPluginLibraries): PluginLibrarySet = lock.withLock {
        val byDigest = installed.index.libraries.associateBy { it.sha256 }
        val acquired = mutableListOf<SharedLibrary>()
        try {
            topologicalOrder(installed.index).forEach { digest ->
                val entry = byDigest.getValue(digest)
                val key = Key(digest, entry.dependsOn.sorted())
                val library = shared.getOrPut(key) {
                    val dependencies = entry.dependsOn.sorted().map { dependencyDigest ->
                        val dependency = byDigest.getValue(dependencyDigest)
                        shared[Key(dependencyDigest, dependency.dependsOn.sorted())]
                            ?: error("Plugin library index is not in dependency order")
                    }
                    val path = installed.paths[digest]
                        ?: error("Plugin library $digest was not materialized")
                    SharedLibrary(
                        key = key,
                        loader = PluginLibraryClassLoader(path, dependencies.map { it.loader }, gateway)
                    )
                }
                library.references++
                acquired += library
            }
            PluginLibrarySet(this, acquired)
        } catch (e: Throwable) {
            releaseLocked(acquired)
            throw e
        }
    }

    internal fun release(libraries: List<SharedLibrary>) = lock.withLock {
        releaseLocked(libraries)
    }

    private fun releaseLocked(libraries: List<SharedLibrary>) {
        libraries.asReversed().forEach { library ->
            library.references--
            if (library.references <= 0) {
                runCatching { library.loader.close() }
                shared.remove(library.key)
            }
        }
    }

    @PreDestroy
    fun close() = lock.withLock {
        shared.values.forEach { runCatching { it.loader.close() } }
        shared.clear()
    }

    private fun topologicalOrder(index: PluginLibraryIndex): List<String> {
        val indegree = index.libraries.associate { it.sha256 to it.dependsOn.size }.toMutableMap()
        val dependents = mutableMapOf<String, MutableList<String>>()
        index.libraries.forEach { library ->
            library.dependsOn.forEach { dependency ->
                dependents.getOrPut(dependency) { mutableListOf() }.add(library.sha256)
            }
        }
        val queue = ArrayDeque(index.libraries.filter { it.dependsOn.isEmpty() }.map { it.sha256 })
        val order = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val digest = queue.removeFirst()
            order += digest
            dependents[digest].orEmpty().forEach { dependent ->
                val remaining = indegree.getValue(dependent) - 1
                indegree[dependent] = remaining
                if (remaining == 0) queue.addLast(dependent)
            }
        }
        check(order.size == index.libraries.size) { "Plugin library index contains a dependency cycle" }
        return order
    }

    /**
     * Identity of a shared library
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Key(
        /**
         * Library digest
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val sha256: String,

        /**
         * Direct dependency digests
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val dependencies: List<String>
    )

    /**
     * One reference-counted shared library
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    class SharedLibrary internal constructor(
        /**
         * Registry key
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val key: Key,

        /**
         * Class loader over the library jar
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val loader: PluginLibraryClassLoader
    ) {
        /**
         * Number of plugin mounts holding this library
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        var references: Int = 0
            internal set
    }
}

/**
 * Libraries held by one plugin mount
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginLibrarySet internal constructor(
    private val registry: PluginLibraryRegistry,
    /**
     * Acquired libraries in dependency order
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val libraries: List<PluginLibraryRegistry.SharedLibrary>
) {
    private val released = AtomicBoolean(false)

    /**
     * Class loaders in dependency order
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val loaders: List<PluginLibraryClassLoader>
        get() = libraries.map { it.loader }

    /**
     * Release this mount's references
     *
     * Idempotent.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun release() {
        if (released.compareAndSet(false, true)) {
            registry.release(libraries)
        }
    }
}
