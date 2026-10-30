package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Status of one module in a fetch task.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "依赖模块获取状态")
enum class PluginModuleFetchModuleStatus {
    /**
     * Not attempted yet
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    PENDING,

    /**
     * Being downloaded
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    DOWNLOADING,

    /**
     * Downloaded and staged by this task
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    INSTALLED,

    /**
     * Already present in the module store
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    EXISTING,

    /**
     * Could not be fetched
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    FAILED,
}
