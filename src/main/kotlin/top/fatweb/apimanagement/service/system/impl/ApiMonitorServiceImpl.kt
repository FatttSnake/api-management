package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.toolkit.Wrappers
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.mapper.system.ApiUsageMapper
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IApiMonitorService
import top.fatweb.apimanagement.service.system.IApiService
import top.fatweb.apimanagement.vo.system.ApiMonitorDashboardVo
import top.fatweb.apimanagement.vo.system.ApiMonitorItemVo
import top.fatweb.apimanagement.vo.system.ApiTopVo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * API monitor service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IApiService
 * @see IApiMonitorService
 */
@Service
@DS("master")
class ApiMonitorServiceImpl(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiUsageMapper: ApiUsageMapper,
    private val apiService: IApiService
) : IApiMonitorService {
    override fun dashboard(): ApiMonitorDashboardVo {
        val prefix = "${serverProperties.security.tokenIssuer}_apimetrics"
        val countKeys = redisProvider.keys("${prefix}_count:*")
        val live = countKeys.mapNotNull { key ->
            val apiCode = key.removePrefix("${prefix}_count:")
            ApiMonitorItemVo(
                apiCode = apiCode,
                count = redisProvider.getObject<String>(key)?.toLongOrNull() ?: 0L,
                error = redisProvider.getObject<String>("${prefix}_error:$apiCode")?.toLongOrNull() ?: 0L,
                latencyMs = redisProvider.getObject<String>("${prefix}_latency:$apiCode")?.toLongOrNull() ?: 0L
            )
        }.sortedByDescending { it.count }

        val activeKeys = redisProvider.getSet<Any>("${prefix}_keys")?.size?.toLong() ?: 0L

        val startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toLocalDateTime()
        val totalToday = apiUsageMapper.selectCount(
            Wrappers.lambdaQuery<ApiUsage>().ge(ApiUsage::createTime, startOfToday)
        )
        val errorToday = apiUsageMapper.selectCount(
            Wrappers.lambdaQuery<ApiUsage>().ge(ApiUsage::createTime, startOfToday).eq(ApiUsage::success, 0)
        )

        return ApiMonitorDashboardVo(
            live = live,
            totalToday = totalToday,
            errorToday = errorToday,
            activeKeys = activeKeys,
            topApis = topApis(10)
        )
    }

    private fun topApis(limit: Int): List<ApiTopVo> {
        val rows = apiUsageMapper.selectMaps(
            QueryWrapper<ApiUsage>()
                .select("api_code", "count(*) as count", "coalesce(sum(cost), 0) as cost")
                .groupBy("api_code")
                .orderByDesc("count")
                .last("limit $limit")
        )
        return rows.map { row ->
            val apiCode = row["api_code"] as? String ?: ""
            ApiTopVo(
                apiCode = apiCode,
                apiName = apiService.getByCode(apiCode)?.name,
                count = (row["count"] as? Number)?.toLong() ?: 0L,
                cost = (row["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
            )
        }
    }
}
