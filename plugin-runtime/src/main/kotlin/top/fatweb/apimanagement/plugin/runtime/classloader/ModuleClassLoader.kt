package top.fatweb.apimanagement.plugin.runtime.classloader

import java.nio.file.Path

/**
 * Class loader for one loader unit: one dependency module or one declared shared-loader group.
 *
 * Own classes are resolved child-first, then direct dependencies, then the platform. A group's
 * members see each other without a declared dependency edge; when two members contain the same
 * class, the jar that sorts first by digest wins. Two plugins that ask for the same unit share
 * one loader instance.
 *
 * @param moduleKey unit identity
 * @param jars jars of the unit, in resolution order
 * @param platform platform class loader
 * @param dependencies dependency loaders
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ModuleClassLoader(
    val moduleKey: ModuleKey,
    jars: List<Path>,
    platform: ClassLoader,
    dependencies: List<ModuleClassLoader>,
) : ChildFirstClassLoader(jars.map { it.toUri().toURL() }.toTypedArray(), platform, dependencies)
