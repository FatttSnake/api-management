package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.EnumValue
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.time.LocalDateTime

/**
 * System log entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_l_sys_log")
class SysLog : Serializable {
    /**
     * Log type enum
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    enum class LogType(@field:EnumValue @field:JsonValue val code: String) {
        INFO("INFO"), ERROR("ERROR"), LOGIN("LOGIN"), LOGOUT("LOGOUT"), REGISTER("REGISTER"), STATISTICS("STATISTICS"), API(
            "API"
        )
    }

    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Log type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LogType
     */
    @TableField("log_type")
    var logType: LogType? = null

    /**
     * Trace ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("trace_id")
    var traceId: String? = null

    /**
     * Operate user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("operate_user_id")
    var operateUserId: Long? = null

    /**
     * Operate time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @TableField("operate_time")
    var operateTime: LocalDateTime? = null

    /**
     * Request URI
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("request_uri")
    var requestUri: String? = null

    /**
     * Request method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("request_method")
    var requestMethod: String? = null

    /**
     * Request parameters
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("request_params")
    var requestParams: String? = null

    /**
     * Request IP
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("request_ip")
    var requestIp: String? = null

    /**
     * Request server address
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("request_server_address")
    var requestServerAddress: String? = null

    /**
     * Is exception
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("exception")
    var exception: Int? = null

    /**
     * Exception information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("exception_info")
    var exceptionInfo: String? = null

    /**
     * Start time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @TableField("start_time")
    var startTime: LocalDateTime? = null

    /**
     * End time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @TableField("end_time")
    var endTime: LocalDateTime? = null

    /**
     * Execute time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("execute_time")
    var executeTime: Long? = null

    /**
     * User agent
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("user_agent")
    var userAgent: String? = null

    /**
     * Operate username
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField(exist = false)
    var operateUsername: String? = null

    override fun toString(): String {
        return "SysLog(id=$id, logType=$logType, traceId=$traceId, operateUserId=$operateUserId, operateTime=$operateTime, requestUri=$requestUri, requestMethod=$requestMethod, requestParams=$requestParams, requestIp=$requestIp, requestServerAddress=$requestServerAddress, exception=$exception, exceptionInfo=$exceptionInfo, startTime=$startTime, endTime=$endTime, executeTime=$executeTime, userAgent=$userAgent, operateUsername=$operateUsername)"
    }
}
