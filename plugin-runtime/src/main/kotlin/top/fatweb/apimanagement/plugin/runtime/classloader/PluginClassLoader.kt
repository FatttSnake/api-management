package top.fatweb.apimanagement.plugin.runtime.classloader

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration

/**
 * Class loader for the plugin jar itself.
 *
 * Plugin classes are resolved child-first, then from the modules in the plugin's lock, then
 * from the platform. The plugin jar never sees a gateway class other than `gateway-api`.
 *
 * @param jar plugin jar
 * @param platform platform class loader
 * @param modules module loaders named by the plugin lock
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginClassLoader(
    jar: Path,
    private val platform: ClassLoader,
    private val modules: List<ModuleClassLoader>
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
                        ?: modules.firstNotNullOfOrNull { module ->
                            try {
                                module.loadClass(name)
                            } catch (_: ClassNotFoundException) {
                                null
                            }
                        }
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
     * Returns a resource from the plugin jar or its modules.
     *
     * @param name resource name
     * @return resource or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResource(name: String): URL? =
        findResource(name)
            ?: modules.firstNotNullOfOrNull { it.getResource(name) }
            ?: platform.getResource(name)

    /**
     * Returns resources from the plugin jar and its modules.
     *
     * @param name resource name
     * @return resources
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResources(name: String): Enumeration<URL> {
        val urls = mutableListOf<URL>()
        findResources(name).asSequence().forEach(urls::add)
        modules.forEach { module -> module.getResources(name).asSequence().forEach(urls::add) }
        platform.getResources(name).asSequence().forEach(urls::add)
        return Collections.enumeration(urls)
    }

    private fun findLocalClass(name: String): Class<*>? =
        try {
            findClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
}
