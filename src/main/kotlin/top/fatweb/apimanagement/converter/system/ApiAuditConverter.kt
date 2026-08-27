package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiAuditVo

/**
 * Convert to ApiAuditVo object
 *
 * @return ApiAuditVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see EventLog
 * @see ApiAuditVo
 */
fun EventLog.toAuditVo() = ApiAuditVo(
    id = this.id,
    event = this.event,
    operateUserId = this.operateUserId,
    operateTime = this.operateTime,
    detail = this.detail
)

/**
 * Convert to PageVo<ApiAuditVo> object
 *
 * @return PageVo<ApiAuditVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see EventLog
 * @see PageVo
 */
fun IPage<EventLog>.toAuditPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(EventLog::toAuditVo)
)
