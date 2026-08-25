package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.system.StatisticsLog
import top.fatweb.apimanagement.mapper.system.StatisticsLogMapper
import top.fatweb.apimanagement.service.system.IStatisticsLogService

/**
 * Statistics log service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see StatisticsLogMapper
 * @see StatisticsLog
 * @see IStatisticsLogService
 */
@DS("sqlite")
@Service
class StatisticsLogServiceImpl : ServiceImpl<StatisticsLogMapper, StatisticsLog>(), IStatisticsLogService
