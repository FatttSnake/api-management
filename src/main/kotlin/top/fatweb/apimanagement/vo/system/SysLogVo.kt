package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.system.SysLog
import java.time.LocalDateTime

/**
 * System log value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "获取系统日志返回参数")
data class SysLogVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Log type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "日志类型")
    val logType: SysLog.LogType?,

    /**
     * Trace ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Trace ID")
    val traceId: String?,

    /**
     * Operate user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "操作用户 ID")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val operateUserId: Long?,

    /**
     * Operate time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "操作时间")
    val operateTime: LocalDateTime?,

    /**
     * Request URI
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求 Uri")
    val requestUri: String?,

    /**
     * Request Method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求方式")
    val requestMethod: String?,

    /**
     * Request parameters
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求参数")
    val requestParams: String?,

    /**
     * Request IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求 IP")
    val requestIp: String?,

    /**
     * Request server address
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求服务器地址")
    val requestServerAddress: String?,

    /**
     * Is exception
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否异常")
    val exception: Boolean?,

    /**
     * Exception information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "异常信息")
    val exceptionInfo: String?,

    /**
     * Start time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "开始时间")
    val startTime: LocalDateTime?,

    /**
     * End time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "结束时间")
    val endTime: LocalDateTime?,

    /**
     * Execute time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "执行时间")
    val executeTime: Long?,

    /**
     * User agent
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "用户代理")
    val userAgent: String?,

    /**
     * Operate username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "操作用户名")
    val operateUsername: String?
)
