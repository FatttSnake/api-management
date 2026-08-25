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
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see EventLogRecord
     */
    fun saveEvent(annotation: EventLogRecord, userId: Long)
}
