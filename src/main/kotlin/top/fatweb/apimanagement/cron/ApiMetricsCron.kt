package top.fatweb.apimanagement.cron

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.system.StatisticsLog
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IStatisticsLogService

/**
 * API metrics scheduled tasks
 *
 * Persists the rolling Redis API metrics window into t_l_statistics_log every minute.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IStatisticsLogService
 */
@Component
class ApiMetricsCron(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val statisticsLogService: IStatisticsLogService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jsonMapper = JsonMapper.builder().build()

    /**
     * Record API request metrics
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Scheduled(cron = "0 * * * * *")
    fun apiMetrics() {
        try {
            val prefix = "${serverProperties.security.tokenIssuer}_apimetrics"
            val countMap = readMetricMap(prefix, "count")
            val errorMap = readMetricMap(prefix, "error")
            val latencyMap = readMetricMap(prefix, "latency")
            val activeKeys = redisProvider.getSet<Any>("${prefix}_keys")?.size ?: 0

            if (countMap.isNotEmpty()) {
                save(StatisticsLog.KeyItem.API_REQUEST_COUNT, countMap)
            }
            if (errorMap.isNotEmpty()) {
                save(StatisticsLog.KeyItem.API_ERROR_COUNT, errorMap)
            }
            if (latencyMap.isNotEmpty()) {
                save(StatisticsLog.KeyItem.API_LATENCY_MS, latencyMap)
            }
            save(StatisticsLog.KeyItem.API_ACTIVE_KEYS, mapOf("activeKeys" to activeKeys))
        } catch (e: Exception) {
            logger.warn("Failed to record API metrics: {}", e.message)
        }
    }

    private fun readMetricMap(prefix: String, suffix: String): Map<String, Long> {
        val keyPrefix = "${prefix}_$suffix:"
        return redisProvider.keys("${keyPrefix}*").associate { key ->
            key.removePrefix(keyPrefix) to (redisProvider.getObject<String>(key)?.toLongOrNull() ?: 0L)
        }
    }

    private fun save(keyItem: StatisticsLog.KeyItem, map: Map<String, Any>) {
        statisticsLogService.save(
            StatisticsLog().apply {
                key = keyItem
                value = jsonMapper.writeValueAsString(map)
            }
        )
    }
}
