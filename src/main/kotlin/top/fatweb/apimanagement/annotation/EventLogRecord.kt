package top.fatweb.apimanagement.annotation

import top.fatweb.apimanagement.entity.system.EventLog

/**
 * Event log record annotation
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventLogRecord(
    val event: EventLog.Event
)
