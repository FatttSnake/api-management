package top.fatweb.apimanagement.cron

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.system.StatisticsLog
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IStatisticsLogService

/**
 * Statistics scheduled tasks
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IStatisticsLogService
 */
@Component
class StatisticsCron(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val statisticsLogService: IStatisticsLogService
) {
    /**
     * Auto record number of online users
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Scheduled(cron = "0 * * * * *")
    fun onlineUserCount() {
        statisticsLogService.save(StatisticsLog().apply {
            key = StatisticsLog.KeyItem.ONLINE_USERS_COUNT
            value = redisProvider.keys("${serverProperties.security.tokenIssuer}_access_*")
                .distinctBy {
                    Regex("${serverProperties.security.tokenIssuer}_access_(.*?)_.*:.*").matchEntire(it)?.groupValues?.getOrNull(
                        1
                    )
                }.size.toString()
        })
    }
}
