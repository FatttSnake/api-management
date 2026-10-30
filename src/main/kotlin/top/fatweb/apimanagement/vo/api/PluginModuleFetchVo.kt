package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Dependency module fetch task.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "依赖模块获取任务")
data class PluginModuleFetchVo(
    /**
     * Task ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "任务 ID")
    val taskId: String,
    /**
     * Task status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "任务状态")
    val status: PluginModuleFetchStatus,
    /**
     * Task level failure message
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "任务失败原因")
    val message: String?,
    /**
     * Modules of the plugin lock
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "依赖模块列表")
    val modules: List<PluginModuleFetchModuleVo>,
    /**
     * Task creation time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "任务创建时间")
    val createTime: LocalDateTime,
    /**
     * Task finish time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "任务完成时间")
    val finishTime: LocalDateTime?,
)
