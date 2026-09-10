package top.fatweb.apimanagement.param.system.apiAudit

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.param.PageSortParam
import java.time.LocalDateTime

/**
 * Get API audit parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API 审计查询请求参数")
data class ApiAuditGetParam(
    /**
     * Event type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see EventLog.Event
     */
    @field:Schema(
        description = "事件类型",
        allowableValues = ["KEY_CREATE", "KEY_UPDATE", "KEY_DELETE", "KEY_STATUS", "KEY_REGENERATE", "KEY_TOPUP"]
    )
    var event: EventLog.Event?,

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
    var endTime: LocalDateTime?
) : PageSortParam()
