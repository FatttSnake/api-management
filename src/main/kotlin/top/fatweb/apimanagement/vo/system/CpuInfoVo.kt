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
     * Show the percentage of CPU utilization
     * that occurred while executing at the user
     * level (application).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "user")
    val user: Long,

    /**
     * Show the percentage of CPU utilization
     * that occurred while executing at the user
     * level with nice priority.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "nice")
    val nice: Long,

    /**
     * Show the percentage of CPU utilization that
     * occurred while executing at the system level
     * (kernel). Note that this does not include time
     * spent servicing hardware and software
     * interrupts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "system")
    val system: Long,

    /**
     * Show the percentage of time that the
     * CPU or CPUs were idle and the system did
     * not have an outstanding disk I/O request.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "idle")
    val idle: Long,

    /**
     * Show the percentage of time that the
     * CPU or CPUs were idle during which the
     * system had an outstanding disk I/O
     * request.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "iowait")
    val iowait: Long,

    /**
     * Show the percentage of time spent by
     * the CPU or CPUs to service hardware
     * interrupts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "irq")
    val irq: Long,

    /**
     * Show the percentage of time spent by
     * the CPU or CPUs to service software
     * interrupts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "softirq")
    val softirq: Long,

    /**
     * Show the percentage of time spent in
     * involuntary wait by the virtual CPU or CPUs
     * while the hypervisor was servicing another
     * virtual processor.
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
