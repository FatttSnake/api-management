package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.system.toAuditPage
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.mapper.system.EventLogMapper
import top.fatweb.apimanagement.param.system.apiAudit.ApiAuditGetParam
import top.fatweb.apimanagement.service.system.IApiAuditService
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiAuditVo

/**
 * API audit service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see EventLogMapper
 * @see EventLog
 * @see IApiAuditService
 */
@Service
@DS("sqlite")
class ApiAuditServiceImpl : ServiceImpl<EventLogMapper, EventLog>(), IApiAuditService {
    override fun getPage(apiAuditGetParam: ApiAuditGetParam?): PageVo<ApiAuditVo> {
        val page = Page<EventLog>(apiAuditGetParam?.currentPage ?: 1, apiAuditGetParam?.pageSize ?: 20)
        setPageSort(apiAuditGetParam, page, OrderItem.desc("operate_time"))

        val wrapper = KtQueryWrapper(EventLog()).apply {
            `in`(
                EventLog::event,
                listOf(
                    EventLog.Event.KEY_CREATE, EventLog.Event.KEY_UPDATE, EventLog.Event.KEY_DELETE,
                    EventLog.Event.KEY_STATUS, EventLog.Event.KEY_REGENERATE, EventLog.Event.KEY_TOPUP
                )
            )
            apiAuditGetParam?.event?.let { eq(EventLog::event, it) }
            apiAuditGetParam?.startTime?.let { ge(EventLog::operateTime, it) }
            apiAuditGetParam?.endTime?.let { le(EventLog::operateTime, it) }
        }

        return page(page, wrapper).toAuditPage()
    }
}
