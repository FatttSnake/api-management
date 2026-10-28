package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import top.fatweb.apimanagement.plugin.runtime.classloader.GatewayApiClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginModuleRegistry
import top.fatweb.apimanagement.plugin.runtime.route.PluginRouteRegistry
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginRuntimeLoader
import top.fatweb.apimanagement.service.api.IPluginModuleService

/**
 * Plugin runtime beans.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class PluginRuntimeConfig {
    /**
     * Platform class loader.
     *
     * @return platform class loader
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginGatewayApiClassLoader(): GatewayApiClassLoader =
        GatewayApiClassLoader(PluginRuntimeConfig::class.java.classLoader)

    /**
     * Route table.
     *
     * @return route registry
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginRouteRegistry(): PluginRouteRegistry = PluginRouteRegistry()

    /**
     * Shared module loader registry.
     *
     * @param platform platform class loader
     * @param moduleService module service
     * @return module registry
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginModuleRegistry(
        platform: GatewayApiClassLoader,
        moduleService: IPluginModuleService
    ): PluginModuleRegistry = PluginModuleRegistry(moduleService.store(), platform)

    /**
     * Plugin loader.
     *
     * @param platform platform class loader
     * @param moduleRegistry module registry
     * @return runtime loader
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Bean
    fun pluginRuntimeLoader(
        platform: GatewayApiClassLoader,
        moduleRegistry: PluginModuleRegistry
    ): PluginRuntimeLoader = PluginRuntimeLoader(platform, moduleRegistry)
}
