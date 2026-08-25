package top.fatweb.apimanagement.service.system.impl

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import oshi.ffm.SystemInfo
import oshi.hardware.CentralProcessor
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.entity.system.StatisticsLog
import top.fatweb.apimanagement.param.system.ActiveInfoGetParam
import top.fatweb.apimanagement.param.system.OnlineInfoGetParam
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IEventLogService
import top.fatweb.apimanagement.service.system.IStatisticsLogService
import top.fatweb.apimanagement.service.system.IStatisticsService
import top.fatweb.apimanagement.util.ByteUtil
import top.fatweb.apimanagement.vo.system.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Statistics service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IStatisticsLogService
 * @see IEventLogService
 * @see IStatisticsService
 */
@Service
class StatisticsServiceImpl(
    private val buildProperties: BuildProperties,
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val statisticsLogService: IStatisticsLogService,
    private val eventLogService: IEventLogService
) : IStatisticsService {
    private val systemProperties: Properties = System.getProperties()
    private val systemInfo: SystemInfo = SystemInfo()
    private val runtime: Runtime = Runtime.getRuntime()

    override fun software() = SoftwareInfoVo(
        serviceVersion = buildProperties.version!!,
        os = systemInfo.operatingSystem.toString(),
        bitness = systemInfo.operatingSystem.bitness,
        javaVersion = systemProperties.getProperty("java.version"),
        javaVersionDate = systemProperties.getProperty("java.version.date"),
        javaVendor = systemProperties.getProperty("java.vendor"),
        javaRuntime = systemProperties.getProperty("java.runtime.name"),
        javaRuntimeVersion = systemProperties.getProperty("java.runtime.version"),
        jvm = systemProperties.getProperty("java.vm.name"),
        jvmVersion = systemProperties.getProperty("java.vm.version"),
        jvmInfo = systemProperties.getProperty("java.vm.info"),
        jvmVendor = systemProperties.getProperty("java.vm.vendor"),
        javaClassVersion = systemProperties.getProperty("java.class.version"),
        osBootTime = LocalDateTime.ofEpochSecond(systemInfo.operatingSystem.systemBootTime, 0, ZoneOffset.UTC),
        serverStartupTime = serverProperties.startupTime
    )

    override fun hardware() = HardwareInfoVo(
        cpu = systemInfo.hardware.processor.processorIdentifier.name,
        arch = systemProperties.getProperty("os.arch"),
        is64Bit = systemInfo.hardware.processor.processorIdentifier.isCpu64bit,
        cpuPhysicalPackageCount = systemInfo.hardware.processor.physicalPackageCount,
        cpuPhysicalProcessorCount = systemInfo.hardware.processor.physicalProcessorCount,
        cpuLogicalProcessorCount = systemInfo.hardware.processor.logicalProcessorCount,
        microarchitecture = systemInfo.hardware.processor.processorIdentifier.microarchitecture,
        memories = "${ByteUtil.formatByteSize(systemInfo.hardware.memory.total)} (${
            if (systemInfo.hardware.memory.physicalMemory.isNotEmpty()) systemInfo.hardware.memory.physicalMemory.joinToString(
                " + "
            ) { ByteUtil.formatByteSize(it.capacity) }
            else "Unknown"
        })",
        disks = if (systemInfo.hardware.diskStores.isNotEmpty()) "${ByteUtil.formatByteSize(systemInfo.hardware.diskStores.sumOf { it.size })} (${
            systemInfo.hardware.diskStores.joinToString(
                " + "
            ) { ByteUtil.formatByteSize(it.size) }
        })"
        else "Unknown"
    )

    override fun cpu(): CpuInfoVo {
        val processor = systemInfo.hardware.processor
        val prevTicks = processor.systemCpuLoadTicks
        val processorPrevTicksList = processor.processorCpuLoadTicks
        TimeUnit.MILLISECONDS.sleep(500)
        val ticks = processor.systemCpuLoadTicks
        val processorTicksList = processor.processorCpuLoadTicks

        val user = ticks[CentralProcessor.TickType.USER.index] - prevTicks[CentralProcessor.TickType.USER.index]
        val nice = ticks[CentralProcessor.TickType.NICE.index] - prevTicks[CentralProcessor.TickType.NICE.index]
        val system = ticks[CentralProcessor.TickType.SYSTEM.index] - prevTicks[CentralProcessor.TickType.SYSTEM.index]
        val idle = ticks[CentralProcessor.TickType.IDLE.index] - prevTicks[CentralProcessor.TickType.IDLE.index]
        val iowait = ticks[CentralProcessor.TickType.IOWAIT.index] - prevTicks[CentralProcessor.TickType.IOWAIT.index]
        val irq = ticks[CentralProcessor.TickType.IRQ.index] - prevTicks[CentralProcessor.TickType.IRQ.index]
        val softirq =
            ticks[CentralProcessor.TickType.SOFTIRQ.index] - prevTicks[CentralProcessor.TickType.SOFTIRQ.index]
        val steal = ticks[CentralProcessor.TickType.STEAL.index] - prevTicks[CentralProcessor.TickType.STEAL.index]
        val total = user + nice + system + idle + iowait + irq + softirq + steal
        return CpuInfoVo(
            user,
            nice,
            system,
            idle,
            iowait,
            irq,
            softirq,
            steal,
            total,
            listOf(*processorPrevTicksList.mapIndexed { index, processorPrevTicks ->
                val processorTicks = processorTicksList[index]
                val processorUser =
                    processorTicks[CentralProcessor.TickType.USER.index] - processorPrevTicks[CentralProcessor.TickType.USER.index]
                val processorNice =
                    processorTicks[CentralProcessor.TickType.NICE.index] - processorPrevTicks[CentralProcessor.TickType.NICE.index]
                val processorSystem =
                    processorTicks[CentralProcessor.TickType.SYSTEM.index] - processorPrevTicks[CentralProcessor.TickType.SYSTEM.index]
                val processorIdle =
                    processorTicks[CentralProcessor.TickType.IDLE.index] - processorPrevTicks[CentralProcessor.TickType.IDLE.index]
                val processorIowait =
                    processorTicks[CentralProcessor.TickType.IOWAIT.index] - processorPrevTicks[CentralProcessor.TickType.IOWAIT.index]
                val processorIrq =
                    processorTicks[CentralProcessor.TickType.IRQ.index] - processorPrevTicks[CentralProcessor.TickType.IRQ.index]
                val processorSoftirq =
                    processorTicks[CentralProcessor.TickType.SOFTIRQ.index] - processorPrevTicks[CentralProcessor.TickType.SOFTIRQ.index]
                val processorSteal =
                    processorTicks[CentralProcessor.TickType.STEAL.index] - processorPrevTicks[CentralProcessor.TickType.STEAL.index]
                val processorTotal =
                    processorUser + processorNice + processorSystem + processorIdle + processorIowait + processorIrq + processorSoftirq + processorSteal
                CpuInfoVo(
                    processorUser,
                    processorNice,
                    processorSystem,
                    processorIdle,
                    processorIowait,
                    processorIrq,
                    processorSoftirq,
                    processorSteal,
                    processorTotal
                )

            }.toTypedArray())
        )
    }

    override fun storage() = StorageInfoVo(
        memoryTotal = systemInfo.hardware.memory.total,
        memoryFree = systemInfo.hardware.memory.available,
        virtualMemoryMax = systemInfo.hardware.memory.virtualMemory.virtualMax,
        virtualMemoryInUse = systemInfo.hardware.memory.virtualMemory.virtualInUse,
        swapTotal = systemInfo.hardware.memory.virtualMemory.swapTotal,
        swapUsed = systemInfo.hardware.memory.virtualMemory.swapUsed,
        jvmTotal = runtime.totalMemory(),
        jvmFree = runtime.freeMemory(),
        fileStores = systemInfo.operatingSystem.fileSystem.fileStores.map {
            FileStoreInfoVo(
                mount = it.mount,
                total = it.totalSpace,
                free = it.freeSpace
            )
        }
    )

    override fun online(onlineInfoGetParam: OnlineInfoGetParam?): OnlineInfoVo {
        val history: List<OnlineInfoVo.HistoryVo> = statisticsLogService.list(
            KtQueryWrapper(StatisticsLog())
                .select(StatisticsLog::value, StatisticsLog::recordTime)
                .eq(StatisticsLog::key, StatisticsLog.KeyItem.ONLINE_USERS_COUNT)
                .between(
                    onlineInfoGetParam?.scope != OnlineInfoGetParam.Scope.ALL,
                    StatisticsLog::recordTime,
                    LocalDateTime.now(ZoneOffset.UTC).run {
                        when (onlineInfoGetParam?.scope) {
                            OnlineInfoGetParam.Scope.DAY -> minusDays(1)
                            OnlineInfoGetParam.Scope.MONTH -> minusMonths(1)
                            OnlineInfoGetParam.Scope.QUARTER -> minusMonths(3)
                            OnlineInfoGetParam.Scope.YEAR -> minusYears(1)
                            OnlineInfoGetParam.Scope.TWO_YEARS -> minusYears(2)
                            OnlineInfoGetParam.Scope.THREE_YEARS -> minusYears(3)
                            OnlineInfoGetParam.Scope.FIVE_YEARS -> minusYears(5)
                            else -> minusWeeks(1)
                        }
                    }.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDateTime.now(ZoneOffset.UTC).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
        ).map {
            OnlineInfoVo.HistoryVo(
                time = it.recordTime!!,
                record = it.value!!
            )
        }

        return OnlineInfoVo(
            current = redisProvider.keys("${serverProperties.security.tokenIssuer}_access_*")
                .distinctBy {
                    Regex("${serverProperties.security.tokenIssuer}_access_(.*?)_.*:.*").matchEntire(it)?.groupValues?.getOrNull(
                        1
                    )
                }.size.toLong(),
            history = history
        )
    }

    override fun active(activeInfoGetParam: ActiveInfoGetParam?): ActiveInfoVo {
        fun getHistory(event: String) = eventLogService.listMaps(
            QueryWrapper<EventLog?>().select("strftime('%Y-%m-%d', operate_time) time, count(distinct operate_user_id) count")
                .eq("event", event).groupBy("time").between(
                    activeInfoGetParam?.scope != ActiveInfoGetParam.Scope.ALL,
                    "operate_time",
                    LocalDateTime.now(ZoneOffset.UTC).run {
                        when (activeInfoGetParam?.scope) {
                            ActiveInfoGetParam.Scope.MONTH -> minusMonths(1)
                            ActiveInfoGetParam.Scope.QUARTER -> minusMonths(3)
                            ActiveInfoGetParam.Scope.YEAR -> minusYears(1)
                            ActiveInfoGetParam.Scope.TWO_YEARS -> minusYears(2)
                            ActiveInfoGetParam.Scope.THREE_YEARS -> minusYears(3)
                            ActiveInfoGetParam.Scope.FIVE_YEARS -> minusYears(5)
                            else -> minusWeeks(1)
                        }
                    }.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDateTime.now(ZoneOffset.UTC).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
        ).map {
            ActiveInfoVo.HistoryVo(
                LocalDate.parse(
                    it["time"] as String,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ), it["count"] as Int
            )
        }

        return ActiveInfoVo(getHistory("REGISTER"), getHistory("LOGIN"), getHistory("VERIFY"))
    }
}
