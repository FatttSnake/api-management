package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.PluginModule
import top.fatweb.apimanagement.param.system.api.PluginModuleGetParam
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleStore
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleVo
import java.time.Duration

/**
 * Plugin dependency module service
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IPluginModuleService : IService<PluginModule> {
    /**
     * Installs every module in a lock and references them from a plugin
     *
     * @param pluginId plugin ID
     * @param lock dependency lock
     * @param bytesOf module bytes by module
     * @return installed modules
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun installForPlugin(
        pluginId: String,
        lock: PluginModuleLock,
        bytesOf: (LockedModule) -> ByteArray?
    ): List<PluginModule>

    /**
     * Stages a module uploaded before its plugin.
     *
     * @param bytes module bytes
     * @return module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun stage(bytes: ByteArray): String

    /**
     * Deletes an unreferenced module.
     *
     * @param sha256 module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(sha256: String)

    /**
     * Points a plugin at the modules of a lock that are already installed
     *
     * @param pluginId plugin ID
     * @param lock dependency lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun reference(pluginId: String, lock: PluginModuleLock)

    /**
     * Removes every module reference held by a plugin
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun release(pluginId: String)

    /**
     * Gets an installed module page.
     *
     * @param param query parameters
     * @return module page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPage(param: PluginModuleGetParam?): PageVo<PluginModuleVo>

    /**
     * Finds one module
     *
     * @param groupId Maven group
     * @param artifactId Maven artifact
     * @param version version
     * @return module or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(groupId: String, artifactId: String, version: String): PluginModule?

    /**
     * Returns digests still referenced by a plugin
     *
     * @return digests
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun referencedDigests(): Set<String>

    /**
     * Deletes unreferenced modules past the grace period
     *
     * @param grace retention period
     * @return number of module files deleted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun gc(grace: Duration = Duration.ZERO): Int

    /**
     * Returns the content-addressed module store
     *
     * @return module store
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun store(): PluginModuleStore
}
