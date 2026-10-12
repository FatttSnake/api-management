package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Storage information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "存储信息返回参数")
data class StorageInfoVo(
    /**
     * The amount of actual physical memory.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总内存容量")
    val memoryTotal: Long,

    /**
     * The amount of physical memory currently
     * available。
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "空闲内存容量")
    val memoryFree: Long,

    /**
     * The maximum memory that can be committed by the system without extending the paging
     * file(s). Also called the Commit Limit.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总虚拟内存容量")
    val virtualMemoryMax: Long,

    /**
     * The memory currently committed by the system, in bytes. Also called the Commit Total.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "已用虚拟内存容量")
    val virtualMemoryInUse: Long,

    /**
     * The current size of the paging/swap file(s).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总交换区容量")
    val swapTotal: Long,

    /**
     * The current memory committed to the
     * paging/swap file(s).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "可用交换区容量")
    val swapUsed: Long,

    /**
     * Total amount of memory in the Java virtual machine.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机总内存容量")
    val jvmTotal: Long,

    /**
     * Amount of free memory in the Java Virtual Machine.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机空闲内存容量")
    val jvmFree: Long,

    /**
     * List of FileStoreInfoVo object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "文件存储信息列表")
    val fileStores: List<FileStoreInfoVo>
)
