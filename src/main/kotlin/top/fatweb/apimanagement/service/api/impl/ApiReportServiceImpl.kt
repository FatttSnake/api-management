package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.mapper.api.ApiUsageMapper
import top.fatweb.apimanagement.param.system.apiReport.ApiReportGetParam
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.service.api.IApiReportService
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.vo.api.ApiReportVo
import top.fatweb.apimanagement.vo.api.ApiTopVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import java.math.BigDecimal

/**
 * Report lookup context used to resolve referenced entities without N+1 queries
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private class ReportContext(
    val keys: Map<Long, ApiKey>,
    val users: Map<Long, UserWithInfoVo>,
    val interfaces: Map<String, ApiInterface>,
    val plugins: Map<String, ApiPlugin>
)

/**
 * API report service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiPluginService
 * @see IApiKeyService
 * @see IUserService
 * @see IStorageBlobService
 * @see IApiReportService
 */
@Service
@DS("master")
class ApiReportServiceImpl(
    private val apiUsageMapper: ApiUsageMapper,
    private val apiPluginService: IApiPluginService,
    private val apiKeyService: IApiKeyService,
    private val userService: IUserService,
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
        val context = buildContext(
            codes = rows.map { it["api_code"] as? String },
            keyIds = rows.map { (it["api_key_id"] as? Number)?.toLong() }
        )
        return rows.map { row -> row.toReportVo(context) }
    }

    override fun cost(apiReportGetParam: ApiReportGetParam?): List<ApiReportVo> {
        val rows = apiUsageMapper.selectMaps(
            baseQuery(apiReportGetParam)
                .select("api_key_id", "api_code", "count(*) as count", "coalesce(sum(cost), 0) as cost")
                .groupBy("api_key_id", "api_code")
                .orderByDesc("cost")
        )
        val context = buildContext(
            codes = rows.map { it["api_code"] as? String },
            keyIds = rows.map { (it["api_key_id"] as? Number)?.toLong() }
        )
        return rows.map { row -> row.toReportVo(context) }
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
        val context = buildContext(
            codes = rows.map { it["api_code"] as? String },
            keyIds = emptyList()
        )
        return rows.map { row -> row.toTopVo(context) }
    }

    override fun export(apiReportGetParam: ApiReportGetParam?): String {
        val rows = usage(apiReportGetParam)
        val csv = StringBuilder("﻿date,apiKeyId,apiCode,apiName,count,cost\n")
        rows.forEach {
            csv.append(
                "${csvField(it.date)},${csvQuoted(it.apiKeyId)},${csvField(it.apiCode)}," +
                    "${csvField(it.apiName)},${csvField(it.count)},${csvField(it.cost)}\n"
            )
        }
        return storageBlobService.saveFile(csv.toString().toByteArray(Charsets.UTF_8))
    }

    private fun baseQuery(apiReportGetParam: ApiReportGetParam?): QueryWrapper<ApiUsage> =
        QueryWrapper<ApiUsage>().apply {
            apiReportGetParam?.apiKeyId?.let { eq("api_key_id", it) }
            apiReportGetParam?.startTime?.let { ge("create_time", it) }
            apiReportGetParam?.endTime?.let { le("create_time", it) }
        }

    private fun buildContext(
        codes: List<String?>,
        keyIds: List<Long?>
    ): ReportContext {
        val codeSet = codes.filterNotNull().toSet()
        val keyIdSet = keyIds.filterNotNull().toSet()

        val keys = if (keyIdSet.isEmpty()) {
            emptyMap()
        } else {
            apiKeyService.listByIds(keyIdSet).associateBy { it.id!! }
        }
        val users = if (keys.isEmpty()) {
            emptyMap()
        } else {
            userService.getBasicInfoByIds(keys.values.mapNotNull { it.userId })
        }
        val interfaces = codeSet
            .associateWith { apiPluginService.getByCode(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }
        val pluginIds = interfaces.values.mapNotNull { it.pluginId }.toSet()
        val plugins = pluginIds
            .associateWith { apiPluginService.getByPluginId(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }

        return ReportContext(keys = keys, users = users, interfaces = interfaces, plugins = plugins)
    }

    private fun MutableMap<String, Any>.toReportVo(context: ReportContext): ApiReportVo {
        val apiCode = this["api_code"] as? String ?: ""
        val apiKeyId = (this["api_key_id"] as? Number)?.toLong()
        val apiInterface = context.interfaces[apiCode]
        val key = apiKeyId?.let { context.keys[it] }

        return ApiReportVo(
            apiKeyId = apiKeyId,
            date = this["date"]?.toString(),
            apiCode = if (apiCode.isEmpty()) null else apiCode,
            apiName = apiInterface?.name,
            count = (this["count"] as? Number)?.toLong() ?: 0L,
            cost = (this["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
            keyVo = key?.toVo(),
            userVo = key?.userId?.let { context.users[it] },
            pluginVo = apiInterface?.pluginId?.let { context.plugins[it]?.toVo() },
            interfaceVo = apiInterface?.toVo()
        )
    }

    private fun MutableMap<String, Any>.toTopVo(context: ReportContext): ApiTopVo {
        val apiCode = this["api_code"] as? String ?: ""
        val apiInterface = context.interfaces[apiCode]

        return ApiTopVo(
            apiCode = if (apiCode.isEmpty()) null else apiCode,
            count = (this["count"] as? Number)?.toLong() ?: 0L,
            cost = (this["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
            pluginVo = apiInterface?.pluginId?.let { context.plugins[it]?.toVo() },
            interfaceVo = apiInterface?.toVo()
        )
    }

    private fun csvField(value: Any?): String {
        val text = value?.toString().orEmpty()
        val escaped = text.replace("\"", "\"\"")
        val needQuote = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needQuote) "\"$escaped\"" else escaped
    }

    private fun csvQuoted(value: Any?): String = "\"${value?.toString().orEmpty().replace("\"", "\"\"")}\""
}
