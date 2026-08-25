package top.fatweb.apimanagement.vo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Page value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "分页返回参数")
data class PageVo<T>(
    /**
     * Total number of records
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总数量", example = "100")
    val total: Long,

    /**
     * Total number of pages
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "总页码", example = "10")
    val pages: Long,

    /**
     * Size of page
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "分页大小", example = "10")
    val size: Long,

    /**
     * Current page number
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "当前页码", example = "2")
    val current: Long,

    /**
     * Records in current page
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "数据")
    val records: List<T>
)
