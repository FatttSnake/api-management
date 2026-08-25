package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * File storage information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "文件存储信息返回参数")
data class FileStoreInfoVo(
    /**
     * Mount point of the File System. The
     * directory users will normally use to
     * interface with the file store.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "挂载点")
    val mount: String,

    /**
     * Total space/capacity of the drive.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总容量")
    val total: Long,

    /**
     * Free space on the drive. This space is
     * unallocated but may require elevated
     * permissions to write.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "空闲容量")
    val free: Long
)
