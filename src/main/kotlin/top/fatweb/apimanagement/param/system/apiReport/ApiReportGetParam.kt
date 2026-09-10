package top.fatweb.apimanagement.param.system.apiReport

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import top.fatweb.apimanagement.annotation.ParamProcessor
import java.time.LocalDateTime

/**
 * Get API report parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 报表查询请求参数")
data class ApiReportGetParam(
    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API Key ID")
    var apiKeyId: Long?,

    /**
     * Start time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "开始时间", example = "2026-01-01T00:00:00.000Z")
    @field:DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    var startTime: LocalDateTime?,

    /**
     * End time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "结束时间", example = "2026-12-31T00:00:00.000Z")
    @field:DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    var endTime: LocalDateTime?,

    /**
     * Limit of top list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Top 数量", defaultValue = "10", example = "10")
    @field:Min(value = 1, message = "Limit must be greater than or equal to 1")
    var limit: Int? = 10
)
