package top.fatweb.apimanagement.plugin.runtime.classloader

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration

/**
 * Class loader for one dependency module.
 *
 * Own classes are resolved child-first, then direct dependencies, then the platform. This lets
 * two plugins use different versions of the same module, while two plugins that ask for the
 * same module share one loader instance.
 *
 * @param moduleKey module identity
 * @param jar module jar
 * @param platform platform class loader
 * @param dependencies direct dependency loaders
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ModuleClassLoader(
    val moduleKey: ModuleKey,
    jar: Path,
    private val platform: ClassLoader,
    private val dependencies: List<ModuleClassLoader>
) : URLClassLoader(arrayOf(jar.toUri().toURL()), platform) {
    /**
     * Loads a class.
     *
     * @param name class name
     * @param resolve whether to resolve the class
     * @return loaded class
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            var loaded = findLoadedClass(name)
            if (loaded == null) {
                loaded = if (GatewayApiClassLoader.isAllowedClass(name)) {
                    platform.loadClass(name)
                } else {
                    findLocalClass(name)
                        ?: dependencies.firstNotNullOfOrNull { it.findLocalOrDependencyClass(name) }
                        ?: platform.loadClass(name)
                }
            }
            if (resolve) {
                resolveClass(loaded)
            }
            return loaded
        }
    }

    /**
     * Returns a resource from this module or its dependencies.
     *
     * @param name resource name
     * @return resource or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResource(name: String): URL? =
        findResource(name)
            ?: dependencies.firstNotNullOfOrNull { it.getResource(name) }
            ?: platform.getResource(name)

    /**
     * Returns resources from this module and its dependencies.
     *
     * @param name resource name
     * @return resources
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResources(name: String): Enumeration<URL> {
        val urls = mutableListOf<URL>()
        findResources(name).asSequence().forEach(urls::add)
        dependencies.forEach { dependency -> dependency.getResources(name).asSequence().forEach(urls::add) }
        platform.getResources(name).asSequence().forEach(urls::add)
        return Collections.enumeration(urls)
    }

    private fun findLocalClass(name: String): Class<*>? =
        try {
            findClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }

    private fun findLocalOrDependencyClass(name: String): Class<*>? =
        findLocalClass(name) ?: dependencies.firstNotNullOfOrNull { it.findLocalOrDependencyClass(name) }
}
