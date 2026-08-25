package top.fatweb.apimanagement.param

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

/**
 * Page sort parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
open class PageSortParam {
    /**
     * Current page number
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "分页页码", defaultValue = "1", example = "1")
    @Min(1, message = "Pagination page number must be a positive integer")
    var currentPage: Long = 1

    /**
     * Size of page
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "分页大小", defaultValue = "20", example = "20")
    @Min(1, message = "The number of data per page must be a positive integer")
    var pageSize: Long = 20

    /**
     * Field name to sort
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "排序字段", example = "id")
    var sortField: String? = null

    /**
     * Sort order by
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "排序方式", allowableValues = ["desc", "asc"], defaultValue = "desc", example = "desc")
    var sortOrder: String? = null
}
