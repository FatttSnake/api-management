package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.toolkit.Wrappers
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.mapper.api.ApiUsageMapper
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiMonitorService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.vo.api.ApiMonitorDashboardVo
import top.fatweb.apimanagement.vo.api.ApiTopVo
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
 * @see IApiPluginService
 * @see IApiMonitorService
 */
@Service
@DS("master")
class ApiMonitorServiceImpl(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val apiUsageMapper: ApiUsageMapper,
    private val apiPluginService: IApiPluginService
) : IApiMonitorService {
    override fun dashboard(): ApiMonitorDashboardVo {
        val prefix = "${serverProperties.security.tokenIssuer}_apimetrics"
        val countKeys = redisProvider.keys("${prefix}_count:*")
        val interfaces = countKeys
            .map { it.removePrefix("${prefix}_count:") }
            .toSet()
            .associateWith { apiPluginService.getByCode(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }
        val plugins = interfaces.values
            .mapNotNull { it.pluginId }
            .toSet()
            .associateWith { apiPluginService.getByPluginId(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }
        val live = countKeys.map { key ->
            val apiCode = key.removePrefix("${prefix}_count:")
            val apiInterface = interfaces[apiCode]
            ApiMonitorDashboardVo.ApiMonitorItemVo(
                apiCode = apiCode,
                count = redisProvider.getObject<Number>(key)?.toLong() ?: 0L,
                error = redisProvider.getObject<Number>("${prefix}_error:$apiCode")?.toLong() ?: 0L,
                latencyMs = redisProvider.getObject<Number>("${prefix}_latency:$apiCode")?.toLong() ?: 0L,
                pluginVo = apiInterface?.pluginId?.let { plugins[it]?.toVo() },
                interfaceVo = apiInterface?.toVo()
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
        val interfaces = rows
            .mapNotNull { it["api_code"] as? String }
            .toSet()
            .associateWith { apiPluginService.getByCode(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }
        val plugins = interfaces.values
            .mapNotNull { it.pluginId }
            .toSet()
            .associateWith { apiPluginService.getByPluginId(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }

        return rows.map { row ->
            val apiCode = row["api_code"] as? String ?: ""
            val apiInterface = interfaces[apiCode]
            ApiTopVo(
                apiCode = if (apiCode.isEmpty()) null else apiCode,
                count = (row["count"] as? Number)?.toLong() ?: 0L,
                cost = (row["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
                pluginVo = apiInterface?.pluginId?.let { plugins[it]?.toVo() },
                interfaceVo = apiInterface?.toVo()
            )
        }
    }
}
