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
 * Creates a child-first [URLClassLoader] for a plugin jar. Well-known prefixes
 * (JDK, Spring, Jackson, Swagger, MyBatis, Kotlin, the gateway and its SDK) are
 * delegated to the parent class loader so their classes keep identical identity;
 * everything else is loaded from the plugin jar first so a plugin may bundle its
 * own third-party dependencies in isolation.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginClassLoaderManager {
    companion object {
        /**
         * Class name prefixes always resolved from the parent (gateway) class loader
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
         * @see URLClassLoader
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
                        val clazz = if (PARENT_FIRST_PREFIXES.any { name.startsWith(it) }) {
                            try {
                                parent.loadClass(name)
                            } catch (_: ClassNotFoundException) {
                                findClass(name)
                            }
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
     * @see Method
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
     * @see RequestMappingInfo
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
     * @see URLClassLoader
     */
    val classLoader: URLClassLoader,

    /**
     * Plugin child Spring context
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GenericApplicationContext
     */
    val context: GenericApplicationContext,

    /**
     * The plugin-facing context bean exposed to the plugin
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginContext
     */
    val pluginContext: PluginContext,

    /**
     * Controller bean instances
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val controllerBeans: List<Any>,

    /**
     * Mounted endpoints
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MountedEndpoint
     */
    val endpoints: List<MountedEndpoint>,

    /**
     * Local path of the plugin jar the class loader reads
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Path
     */
    val tempJarPath: Path,

    /**
     * SHA-256 of the jar content
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val fileHash: String,

    /**
     * Datasources supplied to the plugin, by declared name
     *
     * Held on the runtime rather than reached through the plugin-facing context: an
     * unmount has to close every pool it opened, and that is a fact about what this
     * mount built, not about what the plugin API happens to expose.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see DataSource
     */
    val datasources: Map<String, DataSource>,

    /**
     * Signer public key fingerprint
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val signerKeyId: String,

    /**
     * Plugin lifecycle implementation, or null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginLifecycle
     */
    val lifecycle: PluginLifecycle?
)
