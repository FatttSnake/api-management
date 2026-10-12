package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Hardware information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "硬件信息返回参数")
data class HardwareInfoVo(
    /**
     * Name of CPU
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "CPU")
    val cpu: String,

    /**
     * Arch of CPU
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "架构")
    val arch: String,

    /**
     * Is CPU 64bit
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否为64位")
    val is64Bit: Boolean,

    /**
     * Number of packages/sockets in the system.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "物理 CPU")
    val cpuPhysicalPackageCount: Int,

    /**
     * Number of physical CPUs/cores available for processing.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "物理核心")
    val cpuPhysicalProcessorCount: Int,

    /**
     * Number of logical CPUs available for processing.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "逻辑核心")
    val cpuLogicalProcessorCount: Int,

    /**
     * Processor's microarchitecture, if known.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "微架构")
    val microarchitecture: String,

    /**
     * Memory information overview
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "内存")
    val memories: String,

    /**
     * Disk information overview
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "存储")
    val disks: String
)
