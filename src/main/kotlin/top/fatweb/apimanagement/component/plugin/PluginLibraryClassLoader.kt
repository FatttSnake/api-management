package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.sdk.plugin.PluginClasspath
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.Collections

/**
 * Class loader for one shared plugin dependency
 *
 * Child-first over its own jar, then its direct dependencies, then the gateway. Consumer
 * jars see the same loader instance, so identical dependencies are defined once across
 * plugins.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginLibraryClassLoader(
    jar: Path,
    private val dependencies: List<PluginLibraryClassLoader>,
    private val gateway: ClassLoader
) : URLClassLoader(arrayOf(jar.toUri().toURL()), gateway) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let {
                if (resolve) resolveClass(it)
                return it
            }
            if (PluginClasspath.PARENT_FIRST_PREFIXES.any { name.startsWith(it) }) {
                return gateway.loadClass(name)
            }
            val clazz = try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                loadFromDependencies(name) ?: gateway.loadClass(name)
            }
            if (resolve) resolveClass(clazz)
            return clazz
        }
    }

    override fun getResource(name: String): URL? {
        findResource(name)?.let { return it }
        dependencies.forEach { dependency -> dependency.getResource(name)?.let { return it } }
        return gateway.getResource(name)
    }

    override fun getResources(name: String): java.util.Enumeration<URL> {
        val urls = linkedSetOf<URL>()
        findResources(name).asSequence().forEach { urls.add(it) }
        dependencies.forEach { dependency ->
            dependency.getResources(name).asSequence().forEach { urls.add(it) }
        }
        gateway.getResources(name).asSequence().forEach { urls.add(it) }
        return Collections.enumeration(urls)
    }

    private fun loadFromDependencies(name: String): Class<*>? {
        dependencies.forEach { dependency ->
            try {
                return dependency.loadClass(name)
            } catch (_: ClassNotFoundException) {
                // Try the next dependency.
            }
        }
        return null
    }
}
