package top.fatweb.apimanagement.param.system.apiAccount

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.system.ApiTransaction
import top.fatweb.apimanagement.param.PageSortParam
import java.time.LocalDateTime

/**
 * Get API transaction parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 流水查询请求参数")
data class ApiTransactionGetParam(
    /**
     * Owner user ID (admin only)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属用户 ID（管理员查询指定用户，不传则查自己）")
    var userId: Long?,

    /**
     * Transaction type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTransaction.Type
     */
    @field:Schema(description = "流水类型", allowableValues = ["TOPUP", "DEDUCT", "REFUND", "ADJUST"])
    var type: ApiTransaction.Type?,

    /**
     * Start time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "开始时间", example = "2026-01-01T00:00:00.000Z")
    var startTime: LocalDateTime?,

    /**
     * End time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "结束时间", example = "2026-12-31T00:00:00.000Z")
    var endTime: LocalDateTime?
) : PageSortParam()
