package top.fatweb.apimanagement.vo.system

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

/**
 * CPU information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "CPU 信息返回参数")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CpuInfoVo(
    /**
     * Percentage of CPU utilization at the user level (application).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "user")
    val user: Long,

    /**
     * Percentage of CPU utilization at the user level with nice priority.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "nice")
    val nice: Long,

    /**
     * Percentage of CPU utilization at the system level (kernel).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "system")
    val system: Long,

    /**
     * Percentage of time the CPU was idle without an outstanding disk I/O request.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "idle")
    val idle: Long,

    /**
     * Percentage of time the CPU was idle with an outstanding disk I/O request.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "iowait")
    val iowait: Long,

    /**
     * Percentage of time spent servicing hardware interrupts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "irq")
    val irq: Long,

    /**
     * Percentage of time spent servicing software interrupts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "softirq")
    val softirq: Long,

    /**
     * Percentage of time spent in involuntary wait by a virtual CPU while the hypervisor was
     * servicing another virtual processor.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "steal")
    val steal: Long,

    /**
     * total = user + nice + system + idle + iowait + irq + softirq + steal
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "total")
    val total: Long,

    /**
     * List of CPU processors information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "处理器列表")
    val processors: List<CpuInfoVo>? = null
)
