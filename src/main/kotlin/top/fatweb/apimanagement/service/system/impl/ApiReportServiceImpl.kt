package top.fatweb.apimanagement.service.system.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.system.ApiUsage
import top.fatweb.apimanagement.mapper.system.ApiUsageMapper
import top.fatweb.apimanagement.param.system.apiReport.ApiReportGetParam
import top.fatweb.apimanagement.service.system.IApiPluginService
import top.fatweb.apimanagement.service.system.IApiReportService
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.vo.system.ApiReportVo
import top.fatweb.apimanagement.vo.system.ApiTopVo
import java.math.BigDecimal

/**
 * API report service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginService
 * @see IStorageBlobService
 * @see IApiReportService
 */
@Service
@DS("master")
class ApiReportServiceImpl(
    private val apiUsageMapper: ApiUsageMapper,
    private val apiPluginService: IApiPluginService,
    private val storageBlobService: IStorageBlobService
) : IApiReportService {
    override fun usage(apiReportGetParam: ApiReportGetParam?): List<ApiReportVo> {
        val rows = apiUsageMapper.selectMaps(
            baseQuery(apiReportGetParam)
                .select(
                    "api_key_id", "date(create_time) as date", "api_code",
                    "count(*) as count", "coalesce(sum(cost), 0) as cost"
                )
                .groupBy("api_key_id", "date(create_time)", "api_code")
                .orderByDesc("date")
        )
        return rows.map(::toReportVo)
    }

    override fun cost(apiReportGetParam: ApiReportGetParam?): List<ApiReportVo> {
        val rows = apiUsageMapper.selectMaps(
            baseQuery(apiReportGetParam)
                .select("api_key_id", "api_code", "count(*) as count", "coalesce(sum(cost), 0) as cost")
                .groupBy("api_key_id", "api_code")
                .orderByDesc("cost")
        )
        return rows.map(::toReportVo)
    }

    override fun top(apiReportGetParam: ApiReportGetParam?): List<ApiTopVo> {
        val limit = apiReportGetParam?.limit ?: 10
        val rows = apiUsageMapper.selectMaps(
            baseQuery(apiReportGetParam)
                .select("api_code", "count(*) as count", "coalesce(sum(cost), 0) as cost")
                .groupBy("api_code")
                .orderByDesc("count")
                .last("limit $limit")
        )
        return rows.map { row ->
            val apiCode = row["api_code"] as? String ?: ""
            ApiTopVo(
                apiCode = apiCode,
                apiName = apiPluginService.getByCode(apiCode)?.name,
                count = (row["count"] as? Number)?.toLong() ?: 0L,
                cost = (row["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
            )
        }
    }

    override fun export(apiReportGetParam: ApiReportGetParam?): String {
        val csv = StringBuilder("date,apiKeyId,apiCode,apiName,count,cost\n")
        usage(apiReportGetParam).forEach {
            csv.append("${it.date},${it.apiKeyId},${it.apiCode},${it.apiName},${it.count},${it.cost}\n")
        }
        return storageBlobService.saveFile(csv.toString())
    }

    private fun baseQuery(apiReportGetParam: ApiReportGetParam?): QueryWrapper<ApiUsage> =
        QueryWrapper<ApiUsage>().apply {
            apiReportGetParam?.apiKeyId?.let { eq("api_key_id", it) }
            apiReportGetParam?.startTime?.let { ge("create_time", it) }
            apiReportGetParam?.endTime?.let { le("create_time", it) }
        }

    private fun toReportVo(row: MutableMap<String, Any>): ApiReportVo {
        val apiCode = row["api_code"] as? String ?: ""
        return ApiReportVo(
            apiKeyId = (row["api_key_id"] as? Number)?.toLong(),
            date = row["date"] as? String,
            apiCode = apiCode,
            apiName = apiPluginService.getByCode(apiCode)?.name,
            count = (row["count"] as? Number)?.toLong() ?: 0L,
            cost = (row["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
        )
    }
}
