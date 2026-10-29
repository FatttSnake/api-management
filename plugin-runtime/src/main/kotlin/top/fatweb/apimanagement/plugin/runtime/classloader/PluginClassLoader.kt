package top.fatweb.apimanagement.plugin.runtime.classloader

import java.nio.file.Path

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
class PluginClassLoader(jar: Path, platform: ClassLoader, modules: List<ModuleClassLoader>) :
    ChildFirstClassLoader(arrayOf(jar.toUri().toURL()), platform, modules)
