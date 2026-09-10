package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.mapper.system.EventLogMapper
import top.fatweb.apimanagement.service.system.IEventLogService

/**
 * Event log service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see EventLogMapper
 * @see EventLog
 * @see IEventLogService
 */
@DS("sqlite")
@Service
class EventLogServiceImpl : ServiceImpl<EventLogMapper, EventLog>(), IEventLogService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun saveEvent(
        annotation: EventLogRecord,
        userId: Long,
        apiKeyId: Long?,
        targetUserId: Long?,
        detail: String?
    ) {
        try {
            save(EventLog().apply {
                event = annotation.event
                operateUserId = userId
                this.apiKeyId = apiKeyId
                this.targetUserId = targetUserId
                this.detail = detail
            })
        } catch (e: Exception) {
            logger.error("Cannot record event!!!", e)
        }
    }
}
