package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.ApiPlugin
import top.fatweb.apimanagement.plugin.runtime.classloader.GatewayApiClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginModuleRegistry
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLockParser
import top.fatweb.apimanagement.api.v1.version.VersionRange
import top.fatweb.apimanagement.api.v1.version.VersionRangeException
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * Loads a plugin jar into the gateway.
 *
 * The loader verifies the descriptor and lock, resolves modules through [PluginModuleRegistry],
 * creates the plugin class loader, instantiates the entry point and runs `onStart` with a
 * staging route collector.
 *
 * @param platform platform class loader
 * @param moduleRegistry shared module registry
 * @param descriptorParser descriptor parser
 * @param lockParser lock parser
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginRuntimeLoader(
    private val platform: GatewayApiClassLoader,
    private val moduleRegistry: PluginModuleRegistry,
    private val descriptorParser: PluginDescriptorParser = PluginDescriptorParser(),
    private val lockParser: PluginModuleLockParser = PluginModuleLockParser()
) {
    /**
     * Loads a plugin.
     *
     * @param pluginJar plugin jar
     * @param services plugin-scoped capabilities
     * @return loaded plugin
     * @throws PluginLoadException when the plugin cannot be loaded
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun load(pluginJar: Path, services: PluginRuntimeServices): LoadedPlugin {
        val descriptorBytes = readEntry(pluginJar, DESCRIPTOR_ENTRY)
            ?: throw PluginLoadException("Plugin jar has no $DESCRIPTOR_ENTRY")
        val descriptor = descriptorParser.parse(descriptorBytes)
        checkRequirements(descriptor, services)
        val lock = readEntry(pluginJar, LOCK_ENTRY)
            ?.let(lockParser::parse)
            ?: PluginModuleLock()

        if (descriptor.dependencies.isNotEmpty() && lock.modules.isEmpty()) {
            throw PluginLoadException(
                "Plugin ${descriptor.pluginId} declares dependencies but has no dependency lock"
            )
        }
        if (descriptor.apiVersion > GatewayApiInfo.major) {
            throw PluginLoadException(
                "Plugin ${descriptor.pluginId} requires API ${descriptor.apiVersion}, " +
                    "gateway supports ${GatewayApiInfo.major}"
            )
        }

        val graph = moduleRegistry.acquire(lock)
        var classLoader: PluginClassLoader? = null
        var collector: RouteCollector? = null
        try {
            classLoader = PluginClassLoader(pluginJar, platform, graph.loaders().toList())
            collector = RouteCollector(descriptor.pluginId, classLoader)
            val context = GatewayContextImpl(descriptor, collector, services)
            val mainType = classLoader.loadClass(descriptor.mainClass)
            if (!ApiPlugin::class.java.isAssignableFrom(mainType)) {
                throw PluginLoadException(
                    "${descriptor.mainClass} does not implement ${ApiPlugin::class.java.name}"
                )
            }
            val instance = mainType.getDeclaredConstructor().newInstance() as ApiPlugin
            withPluginClassLoader(classLoader) { instance.onStart(context) }
            val routes = collector.freeze()
            return LoadedPlugin(descriptor, instance, classLoader, graph, routes, context)
        } catch (ex: Throwable) {
            collector?.close()
            classLoader?.close()
            graph.close()
            if (ex is PluginLoadException) {
                throw ex
            }
            throw PluginLoadException(
                "Cannot load plugin ${descriptor.pluginId}: ${ex.message}",
                ex
            )
        }
    }

    private fun checkRequirements(descriptor: PluginDescriptor, services: PluginRuntimeServices) {
        val gatewayVersion = services.gatewayVersion.substringBefore('-').ifBlank { services.gatewayVersion }
        val gatewayApiVersion =
            services.gatewayApiVersion.substringBefore('-').ifBlank { services.gatewayApiVersion }
        try {
            if (!VersionRange.parse(descriptor.requiresGateway).contains(gatewayVersion)) {
                throw PluginLoadException(
                    "Plugin ${descriptor.pluginId} requires gateway ${descriptor.requiresGateway}, " +
                        "gateway is ${services.gatewayVersion}"
                )
            }
            if (!VersionRange.parse(descriptor.requiresGatewayApi).contains(gatewayApiVersion)) {
                throw PluginLoadException(
                    "Plugin ${descriptor.pluginId} requires gateway API ${descriptor.requiresGatewayApi}, " +
                        "gateway API is ${services.gatewayApiVersion}"
                )
            }
        } catch (ex: VersionRangeException) {
            throw PluginLoadException(
                "Invalid version requirement in plugin ${descriptor.pluginId}: ${ex.message}"
            )
        }
    }

    private fun readEntry(jar: Path, name: String): ByteArray? =
        JarFile(jar.toFile()).use { file ->
            file.getJarEntry(name)?.let { entry -> file.getInputStream(entry).readBytes() }
        }

    private fun withPluginClassLoader(classLoader: ClassLoader, action: () -> Unit) {
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = classLoader
        try {
            action()
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
    }

    companion object {
        /**
         * Descriptor entry.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val DESCRIPTOR_ENTRY: String = "META-INF/api-plugin.json"

        /**
         * Dependency lock entry.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val LOCK_ENTRY: String = "META-INF/api-plugin-deps.json"
    }
}

/**
 * Plugin loading failure.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
