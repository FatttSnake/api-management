package top.fatweb.apimanagement.plugin.runtime.classloader

import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration

/**
 * A class loader that prefers its own jar over the platform.
 *
 * Resolution order is: own classes, then the direct dependency loaders in declaration order,
 * then the platform. The platform is the only path to a gateway class, and it only answers for
 * the packages the gateway exposes.
 *
 * @param urls URLs of the jar this loader owns
 * @param platform platform class loader
 * @param dependencies direct dependency loaders
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class ChildFirstClassLoader(
    urls: Array<URL>,
    protected val platform: ClassLoader,
    private val dependencies: List<ChildFirstClassLoader>,
) : URLClassLoader(urls, platform) {
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
                loaded =
                    if (GatewayApiClassLoader.isAllowedClass(name)) {
                        platform.loadClass(name)
                    } else {
                        findLocalOrDependencyClass(name) ?: platform.loadClass(name)
                    }
            }
            if (resolve) {
                resolveClass(loaded)
            }
            return loaded
        }
    }

    /**
     * Returns a resource from this jar or its dependencies.
     *
     * @param name resource name
     * @return resource or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResource(name: String): URL? = findResource(name)
        ?: dependencies.firstNotNullOfOrNull { it.getResource(name) }
        ?: platform.getResource(name)

    /**
     * Returns resources from this jar and its dependencies.
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

    /**
     * Finds a class in this jar or in a dependency, without consulting the platform.
     *
     * @param name class name
     * @return class or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    protected fun findLocalOrDependencyClass(name: String): Class<*>? = synchronized(getClassLoadingLock(name)) {
        findLocalClass(name)
            ?: dependencies.firstNotNullOfOrNull { it.findLocalOrDependencyClass(name) }
    }

    private fun findLocalClass(name: String): Class<*>? =
        // A module reached through two dependency paths is asked twice; defining its class a
        // second time would throw LinkageError, so an already loaded class always wins
        findLoadedClass(name) ?: try {
            findClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
}
