package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.vo.api.PluginModuleFetchVo

/**
 * Plugin dependency module fetch service
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IPluginModuleFetchService {
    /**
     * Starts downloading the modules of a plugin jar that are not in the module store.
     *
     * @param jarBytes plugin jar
     * @return task snapshot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun start(jarBytes: ByteArray): PluginModuleFetchVo

    /**
     * Gets a task snapshot.
     *
     * @param taskId task ID
     * @return task snapshot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(taskId: String): PluginModuleFetchVo

    /**
     * Downloads every missing module of a lock before an install.
     *
     * @param lock dependency lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun ensure(lock: PluginModuleLock)
}
