package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.entity.system.EventLog

/**
 * Event log service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see EventLog
 */
interface IEventLogService : IService<EventLog> {
    /**
     * Save event
     *
     * @param annotation Annotation
     * @param userId User ID
     * @param apiKeyId Affected API key ID (kept as reference, resolved at query time)
     * @param targetUserId Target user ID (kept as reference, resolved at query time)
     * @param detail Event detail text
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see EventLogRecord
     */
    fun saveEvent(
        annotation: EventLogRecord,
        userId: Long,
        apiKeyId: Long? = null,
        targetUserId: Long? = null,
        detail: String? = null
    )
}
