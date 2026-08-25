package top.fatweb.apimanagement.converter.system

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.SysLog
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.SysLogVo

/**
 * Convert to PageVo<SysLogVo> object
 *
 * @return PageVo<SysLogVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see SysLog
 * @see PageVo
 */
fun IPage<SysLog>.toVo() = PageVo(
    this.total,
    this.pages,
    this.size,
    this.current,
    this.records.map { sysLog ->
        SysLogVo(
            id = sysLog.id,
            logType = sysLog.logType,
            traceId = sysLog.traceId,
            operateUserId = sysLog.operateUserId,
            operateTime = sysLog.operateTime,
            requestUri = sysLog.requestUri,
            requestMethod = sysLog.requestMethod,
            requestParams = sysLog.requestParams,
            requestIp = sysLog.requestIp,
            requestServerAddress = sysLog.requestServerAddress,
            exception = sysLog.exception?.let { it == 1 },
            exceptionInfo = sysLog.exceptionInfo,
            startTime = sysLog.startTime,
            endTime = sysLog.endTime,
            executeTime = sysLog.executeTime,
            userAgent = sysLog.userAgent,
            operateUsername = sysLog.operateUsername
        )
    }
)
