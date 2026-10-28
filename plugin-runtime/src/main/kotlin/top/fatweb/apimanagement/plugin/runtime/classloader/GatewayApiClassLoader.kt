package top.fatweb.apimanagement.plugin.runtime.classloader

import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * The platform class loader every plugin and module sees.
 *
 * Only the JDK, Kotlin stdlib and `gateway-api` are delegated to the gateway application class
 * loader. Everything else is invisible: a plugin that references Spring, Jackson, MyBatis or a
 * gateway service class gets a [ClassNotFoundException] instead of accidentally binding to the
 * gateway's copy.
 *
 * @param delegate gateway application class loader
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class GatewayApiClassLoader(
    private val delegate: ClassLoader
) : ClassLoader(null) {
    /**
     * Loads a platform class.
     *
     * @param name class name
     * @param resolve whether to resolve the class
     * @return loaded class
     * @throws ClassNotFoundException when the class is outside the platform surface
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (!isAllowedClass(name)) {
            throw ClassNotFoundException("$name is not part of the gateway platform surface")
        }
        val loaded = findLoadedClass(name) ?: delegate.loadClass(name)
        if (resolve) {
            resolveClass(loaded)
        }
        return loaded
    }

    /**
     * Returns a platform resource.
     *
     * @param name resource name
     * @return resource or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResource(name: String): URL? =
        if (isAllowedResource(name)) delegate.getResource(name) else null

    /**
     * Returns platform resources.
     *
     * @param name resource name
     * @return resources
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun getResources(name: String): Enumeration<URL> =
        if (isAllowedResource(name)) delegate.getResources(name) else Collections.emptyEnumeration()

    companion object {
        /**
         * Class prefixes visible to plugins.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val ALLOWED_CLASS_PREFIXES: List<String> = listOf(
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "kotlin.",
            "top.fatweb.apimanagement.api."
        )

        private val ALLOWED_RESOURCE_PREFIXES: List<String> = listOf(
            "top/fatweb/apimanagement/api/",
            "META-INF/gateway-api.abi.json"
        )

        /**
         * Checks whether a class belongs to the platform surface.
         *
         * @param name class name
         * @return true when allowed
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun isAllowedClass(name: String): Boolean =
            ALLOWED_CLASS_PREFIXES.any { name.startsWith(it) }

        private fun isAllowedResource(name: String): Boolean =
            ALLOWED_RESOURCE_PREFIXES.any { name.startsWith(it) }
    }
}
