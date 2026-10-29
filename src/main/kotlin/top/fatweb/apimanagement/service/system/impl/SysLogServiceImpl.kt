package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.system.toVo
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.entity.system.SysLog
import top.fatweb.apimanagement.mapper.system.SysLogMapper
import top.fatweb.apimanagement.param.system.SysLogGetParam
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.service.system.ISysLogService
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.SysLogVo

/**
 * System log service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DS("sqlite")
@Service
class SysLogServiceImpl(private val userService: IUserService) :
    ServiceImpl<SysLogMapper, SysLog>(),
    ISysLogService {
    override fun getPage(sysLogGetParam: SysLogGetParam?): PageVo<SysLogVo> {
        val sysLogPage = Page<SysLog>(sysLogGetParam?.currentPage ?: 1, sysLogGetParam?.pageSize ?: 20)

        setPageSort(sysLogGetParam, sysLogPage, OrderItem.desc("start_time"))

        val sysLogIPage =
            baseMapper.selectPage(
                page = sysLogPage,
                logType = sysLogGetParam?.logType?.split(","),
                traceId = sysLogGetParam?.searchTraceId,
                requestMethod = sysLogGetParam?.requestMethod?.split(","),
                searchRequestUrl = sysLogGetParam?.searchRequestUrl?.let { "%$it%" },
                searchStartTime = sysLogGetParam?.searchStartTime,
                searchEndTime = sysLogGetParam?.searchEndTime,
            )

        if (sysLogIPage.records.isNotEmpty()) {
            val userIds = sysLogIPage.records.map(SysLog::operateUserId)

            val usernames =
                userService
                    .list(KtQueryWrapper(User()).select(User::id, User::username).`in`(User::id, userIds))
                    .associate { it.id to it.username }

            sysLogIPage.records.forEach { it.operateUsername = usernames[it.operateUserId] }
        }

        return sysLogIPage.toVo()
    }
}
