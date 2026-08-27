package top.fatweb.apimanagement.param.system.apiUsage

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam
import java.time.LocalDateTime

/**
 * Get API usage parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 用量查询请求参数")
data class ApiUsageGetParam(
    /**
     * Owner user ID (admin only)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属用户 ID（管理员查询指定用户）")
    var userId: Long?,

    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API Key ID")
    var apiKeyId: Long?,

    /**
     * API code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 编码", example = "api:v1:avatar:getRandom")
    var apiCode: String?,

    /**
     * Success status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否成功", allowableValues = ["true", "false"])
    var success: Boolean?,

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
