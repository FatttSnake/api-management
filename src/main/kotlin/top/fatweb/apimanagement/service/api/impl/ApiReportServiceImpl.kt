package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.api.toVo
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.exception.ExportTooManyRecordsException
import top.fatweb.apimanagement.mapper.api.ApiUsageMapper
import top.fatweb.apimanagement.param.system.apiReport.ApiReportGetParam
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.service.api.IApiPluginService
import top.fatweb.apimanagement.service.api.IApiReportService
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.util.TimezoneUtil
import top.fatweb.apimanagement.vo.api.ApiKeyVo
import top.fatweb.apimanagement.vo.api.ApiReportVo
import top.fatweb.apimanagement.vo.api.ApiTopVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Maximum row count of a detail export
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private const val MAX_DETAIL_EXPORT_ROWS = 100_000L

/**
 * UTF-8 byte order mark, required by Excel to detect the encoding of the exported CSV
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private const val CSV_BOM = "﻿"

/**
 * Header of the usage summary export, keep in sync with the columns built in [ApiReportServiceImpl.export]
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private const val USAGE_EXPORT_HEADER = "日期,插件,接口,API编码,请求路径,请求方法,所属用户,Key,调用次数,费用"

/**
 * Header of the usage detail export, keep in sync with the columns built in [ApiReportServiceImpl.exportDetail]
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private const val DETAIL_EXPORT_HEADER =
    "调用时间,插件,接口,API编码,请求路径,请求方法,响应码,结果,执行耗时(ms),请求IP,Trace ID,所属用户,Key,计费金额,计费模式"

/**
 * Formatter of the time column of the detail export
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
private val CSV_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
        val dateExpression = localDateExpression()
        val rows = apiUsageMapper.selectMaps(
            baseQuery(apiReportGetParam)
                .select(
                    "api_key_id", "$dateExpression as date", "api_code",
                    "count(*) as count", "coalesce(sum(cost), 0) as cost"
                )
                .groupBy("api_key_id", dateExpression, "api_code")
                .orderByDesc("date")
                .orderByAsc("api_code", "api_key_id")
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
        val csv = StringBuilder(CSV_BOM + USAGE_EXPORT_HEADER + "\n")
        rows.forEach { row ->
            csv.append(
                csvLine(
                    row.date,
                    row.pluginVo?.name,
                    row.interfaceVo?.name,
                    row.apiCode,
                    row.interfaceVo?.path,
                    row.interfaceVo?.method,
                    csvUser(row.userVo),
                    csvKey(row.keyVo),
                    row.count,
                    csvMoney(row.cost)
                )
            )
        }
        return storageBlobService.saveFile(csv.toString().toByteArray(Charsets.UTF_8))
    }

    override fun exportDetail(apiReportGetParam: ApiReportGetParam?): String {
        val total = apiUsageMapper.selectCount(baseQuery(apiReportGetParam))
        if (total > MAX_DETAIL_EXPORT_ROWS) {
            throw ExportTooManyRecordsException(MAX_DETAIL_EXPORT_ROWS)
        }

        val usages = apiUsageMapper.selectList(
            baseQuery(apiReportGetParam).orderByDesc("create_time")
        )
        val context = buildContext(
            codes = usages.map { it.apiCode },
            keyIds = usages.map { it.apiKeyId },
            userIds = usages.map { it.userId }
        )

        val csv = StringBuilder(CSV_BOM + DETAIL_EXPORT_HEADER + "\n")
        usages.forEach { usage ->
            val apiInterface = usage.apiCode?.let { context.interfaces[it] }
            val key = usage.apiKeyId?.let { context.keys[it] }
            val user = usage.userId?.let { context.users[it] }
                ?: key?.userId?.let { context.users[it] }
            csv.append(
                csvLine(
                    csvLocalTime(usage.createTime),
                    apiInterface?.pluginId?.let { context.plugins[it] }?.name,
                    apiInterface?.name,
                    usage.apiCode,
                    usage.requestPath,
                    usage.requestMethod,
                    usage.responseCode,
                    if (usage.success == 1) "成功" else "失败",
                    usage.executeTime,
                    usage.requestIp,
                    usage.traceId,
                    csvUser(user),
                    csvKey(key?.toVo()),
                    csvMoney(usage.cost),
                    csvBillingMode(usage.billingMode)
                )
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

    /**
     * Build the expression of the local date of the request creator, the offset comes from
     * [TimezoneUtil] so that the day boundary matches the one shown in the console
     *
     * @return SQL expression of the local date
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimezoneUtil
     */
    private fun localDateExpression(): String =
        "date(date_add(create_time, interval ${TimezoneUtil.offsetMinutes()} minute))"

    private fun buildContext(
        codes: List<String?>,
        keyIds: List<Long?>,
        userIds: List<Long?> = emptyList()
    ): ReportContext {
        val codeSet = codes.filterNotNull().toSet()
        val keyIdSet = keyIds.filterNotNull().toSet()

        val keys = if (keyIdSet.isEmpty()) {
            emptyMap()
        } else {
            apiKeyService.listByIds(keyIdSet).associateBy { it.id!! }
        }
        val relatedUserIds = (keys.values.mapNotNull { it.userId } + userIds.filterNotNull()).distinct()
        val users = if (relatedUserIds.isEmpty()) {
            emptyMap()
        } else {
            userService.getBasicInfoByIds(relatedUserIds)
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
            apiCode = apiCode.ifEmpty { null },
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
            apiCode = apiCode.ifEmpty { null },
            count = (this["count"] as? Number)?.toLong() ?: 0L,
            cost = (this["cost"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
            pluginVo = apiInterface?.pluginId?.let { context.plugins[it]?.toVo() },
            interfaceVo = apiInterface?.toVo()
        )
    }

    private fun csvLine(vararg values: Any?): String =
        values.joinToString(",") { csvField(it) } + "\n"

    /**
     * Convert the stored UTC time to the time zone of the client
     *
     * @param time Stored UTC time
     * @return Formatted local time
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimezoneUtil
     */
    private fun csvLocalTime(time: LocalDateTime?): String =
        time?.plusMinutes(TimezoneUtil.offsetMinutes().toLong())?.format(CSV_TIME_FORMATTER).orEmpty()

    private fun csvMoney(value: BigDecimal?): String =
        (value ?: BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP).toPlainString()

    private fun csvUser(user: UserWithInfoVo?): String {
        val username = user?.username.orEmpty()
        val nickname = user?.userInfo?.nickname.orEmpty()
        return when {
            nickname.isEmpty() -> username
            username.isEmpty() -> nickname
            else -> "$nickname($username)"
        }
    }

    private fun csvKey(key: ApiKeyVo?): String {
        val accessKey = key?.accessKey.orEmpty()
        val name = key?.name.orEmpty()
        return when {
            name.isEmpty() -> accessKey
            accessKey.isEmpty() -> name
            else -> "$accessKey($name)"
        }
    }

    private fun csvBillingMode(billingMode: ApiInterface.BillingMode?): String = when (billingMode) {
        ApiInterface.BillingMode.FREE -> "免费"
        ApiInterface.BillingMode.SUCCESS_ONLY -> "仅成功"
        ApiInterface.BillingMode.ALWAYS -> "总是"
        null -> ""
    }

    private fun csvField(value: Any?): String {
        val text = value?.toString().orEmpty()
        val escaped = text.replace("\"", "\"\"")
        val needQuote = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needQuote) "\"$escaped\"" else escaped
    }
}
