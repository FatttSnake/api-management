package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import java.time.LocalDateTime

/**
 * API audit value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 审计返回参数")
data class ApiAuditVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Event type
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see EventLog.Event
     */
    @field:Schema(description = "事件类型")
    val event: EventLog.Event?,

    /**
     * Operate user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
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
     * Event detail
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "事件详情")
    val detail: String?,

    /**
     * Operator user information (resolved from operateUserId)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserWithInfoVo
     */
    @field:Schema(description = "操作人信息")
    val userVo: UserWithInfoVo?,

    /**
     * Affected API key information (resolved from apiKeyId)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyVo
     */
    @field:Schema(description = "被操作 Key 信息")
    val keyVo: ApiKeyVo?,

    /**
     * Target user information (resolved from targetUserId)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserWithInfoVo
     */
    @field:Schema(description = "目标用户信息")
    val targetUserVo: UserWithInfoVo?
)
