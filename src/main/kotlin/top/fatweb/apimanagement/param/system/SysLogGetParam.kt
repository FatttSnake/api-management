package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam
import java.time.LocalDateTime

/**
 * Get system log parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "获取系统日志请求参数")
data class SysLogGetParam(
    /**
     * Log type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "类型过滤(多个使用逗号分隔)",
        allowableValues = ["INFO", "LOGIN", "LOGOUT", "REGISTER", "STATISTICS", "API", "ERROR"],
        example = "INFO"
    )
    val logType: String?,

    /**
     * Trace ID to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "查询 Trace ID",
        example = "2c5898e1d67d47768aa1942afada1823"
    )
    val searchTraceId: String?,

    /**
     * Request method to filter
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "请求方式过滤(多个使用逗号分隔)",
        allowableValues = ["GET", "POST", "PUT", "PATCH", "DELETE", "DELETE", "OPTIONS"],
        example = "GET,POST"
    )
    val requestMethod: String?,

    /**
     * Request URL to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询请求 Url")
    var searchRequestUrl: String?,

    /**
     * Start time to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "查询开始时间", example = "1900-01-01T00:00:00.000Z")
    @field:DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val searchStartTime: LocalDateTime?,

    /**
     * End time to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "查询结束时间", example = "1900-01-01T00:00:00.000Z")
    @field:DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val searchEndTime: LocalDateTime?
) : PageSortParam()
