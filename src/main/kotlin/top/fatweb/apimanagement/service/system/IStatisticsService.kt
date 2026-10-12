package top.fatweb.apimanagement.service.system

import top.fatweb.apimanagement.param.system.ActiveInfoGetParam
import top.fatweb.apimanagement.param.system.OnlineInfoGetParam
import top.fatweb.apimanagement.vo.system.*

/**
 * Statistics service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IStatisticsService {
    /**
     * Get software information
     *
     * @return SoftwareInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun software(): SoftwareInfoVo

    /**
     * Get hardware information
     *
     * @return HardwareInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun hardware(): HardwareInfoVo

    /**
     * Get CPU information
     *
     * @return CpuInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun cpu(): CpuInfoVo

    /**
     * Get storage information
     *
     * @return StorageInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun storage(): StorageInfoVo

    /**
     * Get the history of online users information
     *
     * @param onlineInfoGetParam Get online information parameters
     * @return OnlineInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun online(onlineInfoGetParam: OnlineInfoGetParam?): OnlineInfoVo

    /**
     * Get the history of active information
     *
     * @param activeInfoGetParam Get active information parameters
     * @return ActiveInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun active(activeInfoGetParam: ActiveInfoGetParam?): ActiveInfoVo
}
