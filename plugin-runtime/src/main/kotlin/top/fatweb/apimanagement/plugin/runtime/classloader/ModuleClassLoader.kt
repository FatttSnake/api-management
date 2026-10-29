package top.fatweb.apimanagement.plugin.runtime.classloader

import java.nio.file.Path

/**
 * Class loader for one dependency module.
 *
 * Own classes are resolved child-first, then direct dependencies, then the platform. This lets
 * two plugins use different versions of the same module, while two plugins that ask for the
 * same module share one loader instance.
 *
 * @param moduleKey module identity
 * @param jar module jar
 * @param platform platform class loader
 * @param dependencies direct dependency loaders
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ModuleClassLoader(
    val moduleKey: ModuleKey,
    jar: Path,
    platform: ClassLoader,
    dependencies: List<ModuleClassLoader>,
) : ChildFirstClassLoader(arrayOf(jar.toUri().toURL()), platform, dependencies)
