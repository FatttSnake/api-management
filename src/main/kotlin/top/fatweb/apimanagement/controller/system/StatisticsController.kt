package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.ActiveInfoGetParam
import top.fatweb.apimanagement.param.system.OnlineInfoGetParam
import top.fatweb.apimanagement.service.system.IStatisticsService
import top.fatweb.apimanagement.vo.system.*

/**
 * Statistics management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IStatisticsService
 */
@BaseController(path = ["/system/statistics"], name = "统计接口", description = "系统信息统计相关接口")
class StatisticsController(
    private val statisticService: IStatisticsService
) {
    /**
     * Get software information
     *
     * @return Response object includes software information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see SoftwareInfoVo
     */
    @Operation(summary = "获取软件信息")
    @GetMapping("/software")
    @PreAuthorize("hasAnyAuthority('system:statistics:base:query')")
    fun software(): ResponseResult<SoftwareInfoVo> = ResponseResult.success(data = statisticService.software())

    /**
     * Get hardware information
     *
     * @return Response object includes hardware information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see HardwareInfoVo
     */
    @Operation(summary = "获取硬件信息")
    @GetMapping("/hardware")
    @PreAuthorize("hasAnyAuthority('system:statistics:base:query')")
    fun hardware(): ResponseResult<HardwareInfoVo> = ResponseResult.success(data = statisticService.hardware())

    /**
     * Get CPU information
     *
     * @return Response object includes CPU information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see CpuInfoVo
     */
    @Operation(summary = "获取 CPU 信息")
    @GetMapping("/cpu")
    @PreAuthorize("hasAnyAuthority('system:statistics:real:query')")
    fun cpu(): ResponseResult<CpuInfoVo> = ResponseResult.success(data = statisticService.cpu())

    /**
     * Get storage information
     *
     * @return Response object includes storage information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see StorageInfoVo
     */
    @Operation(summary = "获取存储信息")
    @GetMapping("/storage")
    @PreAuthorize("hasAnyAuthority('system:statistics:real:query')")
    fun storage(): ResponseResult<StorageInfoVo> = ResponseResult.success(data = statisticService.storage())

    /**
     * Get the history of online users information
     *
     * @param onlineInfoGetParam Get online information parameters
     * @return Response object includes online user information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see OnlineInfoGetParam
     * @see ResponseResult
     * @see OnlineInfoVo
     */
    @Operation(summary = "获取在线用户数量信息")
    @GetMapping("/online")
    @PreAuthorize("hasAnyAuthority('system:statistics:usage:query')")
    fun online(onlineInfoGetParam: OnlineInfoGetParam?): ResponseResult<OnlineInfoVo> =
        ResponseResult.success(data = statisticService.online(onlineInfoGetParam))

    /**
     * Get the history of active information
     *
     * @param activeInfoGetParam Get active information parameters
     * @return Response object includes history of active information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ActiveInfoGetParam
     * @see ResponseResult
     * @see ActiveInfoVo
     */
    @Operation(summary = "获取用户活跃信息")
    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('system:statistics:usage:query')")
    fun active(activeInfoGetParam: ActiveInfoGetParam): ResponseResult<ActiveInfoVo> =
        ResponseResult.success(data = statisticService.active(activeInfoGetParam))
}
