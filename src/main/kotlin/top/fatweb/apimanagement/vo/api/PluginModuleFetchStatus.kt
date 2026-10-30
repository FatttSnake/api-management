package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Status of a dependency module fetch task.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "依赖模块获取任务状态")
enum class PluginModuleFetchStatus {
    /**
     * Modules are being downloaded
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    RUNNING,

    /**
     * Every module is available
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    SUCCESS,

    /**
     * At least one module could not be fetched
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    FAILED,
}
