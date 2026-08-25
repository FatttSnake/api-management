package top.fatweb.apimanagement.mapper.system

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.system.StatisticsLog

/**
 * Statistics log mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see StatisticsLog
 */
@Mapper
interface StatisticsLogMapper : BaseMapper<StatisticsLog>
