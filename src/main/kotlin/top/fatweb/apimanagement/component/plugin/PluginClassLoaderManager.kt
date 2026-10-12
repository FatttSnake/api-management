package top.fatweb.apimanagement.component.plugin

import org.springframework.context.support.GenericApplicationContext
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import top.fatweb.apimanagement.sdk.plugin.PluginContext
import top.fatweb.apimanagement.sdk.plugin.PluginLifecycle
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile
import javax.sql.DataSource

/**
 * Plugin class loader manager
 *
 * Creates a child-first [URLClassLoader] for a plugin jar: classes under the well-known
 * prefixes resolve from the parent so their identity is shared, and everything else is loaded
 * from the plugin jar first.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginClassLoaderManager {
    companion object {
        /**
         * Class name prefixes always resolved from the parent (gateway) class loader, with no
         * fallback to the plugin jar
         */
        private val PARENT_FIRST_PREFIXES = listOf(
            "java.",
            "javax.",
            "jakarta.",
            "sun.",
            "jdk.",
            "org.springframework.",
            "org.slf4j.",
            "tools.jackson.",
            "com.fasterxml.",
            "io.swagger.",
            "org.springdoc.",
            "com.baomidou.",
            "com.zaxxer.hikari.",
            "com.mysql.cj.jdbc.",
            "org.sqlite.",
            "kotlin.",
            "kotlinx.",
            "top.fatweb.apimanagement."
        )

        /**
         * Create a child-first class loader over a plugin jar
         *
         * @param jarUrl URL of the plugin jar
         * @return Class loader
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun create(jarUrl: URL): URLClassLoader {
            val parent = Thread.currentThread().contextClassLoader
                ?: PluginClassLoaderManager::class.java.classLoader
            return object : URLClassLoader(arrayOf(jarUrl), parent) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    synchronized(getClassLoadingLock(name)) {
                        findLoadedClass(name)?.let {
                            if (resolve) resolveClass(it)
                            return it
                        }
                        // Shared prefixes come from the parent alone: deliberately no fallback to
                        // the jar, so a plugin cannot shadow a class the gateway owns. A plugin
                        // that needs one of these and does not find it has to stop bundling it.
                        val clazz = if (PARENT_FIRST_PREFIXES.any { name.startsWith(it) }) {
                            parent.loadClass(name)
                        } else {
                            try {
                                findClass(name)
                            } catch (_: ClassNotFoundException) {
                                parent.loadClass(name)
                            }
                        }
                        if (resolve) resolveClass(clazz)
                        return clazz
                    }
                }
            }
        }

        /**
         * Run [block] with the thread context class loader set to [loader], restoring it after
         *
         * @param loader Class loader to install
         * @param block Work to run under it
         * @return Whatever [block] returned
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun <T> withPluginClassLoader(loader: ClassLoader, block: () -> T): T {
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = loader
            return try {
                block()
            } finally {
                thread.contextClassLoader = previous
            }
        }

        /**
         * Compute the root packages of all classes in a jar, i.e. the packages that
         * are not a sub-package of another package present in the jar. Used as the
         * component-scan base packages of the plugin's child context.
         *
         * @param jarPath Plugin jar path
         * @return Set of root package names
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun rootPackages(jarPath: Path): Set<String> {
            val packages = mutableSetOf<String>()
            JarFile(jarPath.toFile()).use { jarFile ->
                jarFile.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory || !entry.name.endsWith(".class")) return@forEach
                    val pkg = entry.name.substringBeforeLast('/', "").replace('/', '.')
                    if (pkg.isNotBlank()) packages.add(pkg)
                }
            }
            return packages.filter { pkg ->
                packages.none { other -> other != pkg && pkg.startsWith("$other.") }
            }.toSet()
        }

        /**
         * Root packages of a jar that lead to one of its controllers
         *
         * The base packages a plugin's child context scans: the plugin's own code lives under
         * these, while a fat jar's bundled-library components sit under roots of their own and
         * are left alone. Also what decides which classes count as the plugin's own when one
         * fails to load, so the install check and the scan agree on the same boundary.
         *
         * @param jarPath Plugin jar path
         * @param controllerClasses Controllers found in the jar
         * @return Set of root package names, possibly empty
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun controllerRootPackages(jarPath: Path, controllerClasses: List<Class<*>>): Set<String> {
            val controllerPackages = controllerClasses.map { it.`package`.name }.toSet()
            return rootPackages(jarPath)
                .filter { root -> controllerPackages.any { it == root || it.startsWith("$root.") } }
                .toSet()
        }
    }
}

/**
 * A mounted (and mapped) endpoint of a plugin
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class MountedEndpoint(
    /**
     * Controller bean instance
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val controller: Any,

    /**
     * Endpoint handler method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val method: Method,

    /**
     * API scoping code, e.g. api:echo:v1:ping
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val code: String,

    /**
     * Endpoint display name (from @Operation.summary, fallback: method name)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val name: String,

    /**
     * Endpoint description (from @Operation.description)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val description: String,

    /**
     * Literal request path, e.g. /api/echo/v1/ping
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val fullPath: String,

    /**
     * HTTP method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val httpMethod: String,

    /**
     * API version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val apiVersion: Int,

    /**
     * The exact RequestMappingInfo registered, needed to unregister later
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val info: RequestMappingInfo
)

/**
 * Runtime state of an installed plugin
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginRuntime(
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: String,

    /**
     * Child-first class loader over the plugin jar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val classLoader: URLClassLoader,

    /**
     * Plugin child Spring context
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val context: GenericApplicationContext,

    /**
     * The plugin-facing context bean exposed to the plugin
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginContext: PluginContext,

    /**
     * Mounted endpoints
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val endpoints: List<MountedEndpoint>,

    /**
     * Local path of the plugin jar the class loader reads
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val tempJarPath: Path,

    /**
     * Datasources supplied to the plugin, by declared name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val datasources: Map<String, DataSource>,

    /**
     * Plugin lifecycle implementation, or null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val lifecycle: PluginLifecycle?
)
