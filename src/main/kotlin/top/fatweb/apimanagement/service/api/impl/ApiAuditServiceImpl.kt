package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.api.toAuditPage
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.mapper.system.EventLogMapper
import top.fatweb.apimanagement.param.system.apiAudit.ApiAuditGetParam
import top.fatweb.apimanagement.service.api.IApiAuditService
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiAuditVo

/**
 * API audit service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see EventLogMapper
 * @see EventLog
 * @see IApiKeyService
 * @see IUserService
 * @see IApiAuditService
 */
@Service
@DS("sqlite")
class ApiAuditServiceImpl(
    private val apiKeyService: IApiKeyService,
    private val userService: IUserService
) : ServiceImpl<EventLogMapper, EventLog>(), IApiAuditService {
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

        val pageResult = page(page, wrapper)
        val records = pageResult.records
        val keyIds = records.mapNotNull { it.apiKeyId }.toSet()
        val keys = if (keyIds.isEmpty()) {
            emptyList()
        } else {
            apiKeyService.listByIds(keyIds)
        }
        val userMap = userService.getBasicInfoByIds(
            (records.flatMap { listOfNotNull(it.operateUserId, it.targetUserId) } + keys.mapNotNull { it.userId })
                .toSet()
                .toList()
        )
        val keyMap = keys.associate { it.id!! to it.toVo() }

        return pageResult.toAuditPage(
            resolveUser = { userId -> userMap[userId] },
            resolveKey = { keyId -> keyMap[keyId] },
            resolveTargetUser = { userId -> userMap[userId] }
        )
    }
}
