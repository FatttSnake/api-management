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
            "java.", "javax.", "jakarta.", "sun.", "jdk.",
            "org.springframework.", "org.slf4j.",
            "tools.jackson.", "com.fasterxml.",
            "io.swagger.", "org.springdoc.",
            "com.baomidou.",
            "kotlin.", "kotlinx.",
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
 * @see PluginRuntime
 */
data class MountedEndpoint(
    /**
     * Controller bean instance
     */
    val controller: Any,

    /**
     * Endpoint handler method
     */
    val method: Method,

    /**
     * API scoping code, e.g. api:echo:v1:ping
     */
    val code: String,

    /**
     * Endpoint name (handler method name)
     */
    val name: String,

    /**
     * Endpoint description (from @ApiController.description)
     */
    val description: String,

    /**
     * Literal request path, e.g. /api/echo/v1/ping
     */
    val fullPath: String,

    /**
     * HTTP method
     */
    val httpMethod: String,

    /**
     * API version
     */
    val apiVersion: Int,

    /**
     * The exact RequestMappingInfo registered, needed to unregister later
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
     */
    val pluginId: String,

    /**
     * Child-first class loader over the plugin jar
     */
    val classLoader: URLClassLoader,

    /**
     * Plugin child Spring context
     */
    val context: GenericApplicationContext,

    /**
     * The plugin-facing context bean exposed to the plugin
     */
    val pluginContext: PluginContext,

    /**
     * Controller bean instances
     */
    val controllerBeans: List<Any>,

    /**
     * Mounted endpoints
     */
    val endpoints: List<MountedEndpoint>,

    /**
     * Local path of the plugin jar the class loader reads
     */
    val tempJarPath: Path,

    /**
     * SHA-256 of the jar content
     */
    val fileHash: String,

    /**
     * Signer public key fingerprint
     */
    val signerKeyId: String,

    /**
     * Plugin lifecycle implementation, or null
     */
    val lifecycle: PluginLifecycle?
)
