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
     * @see SoftwareInfoVo
     */
    fun software(): SoftwareInfoVo

    /**
     * Get hardware information
     *
     * @return HardwareInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HardwareInfoVo
     */
    fun hardware(): HardwareInfoVo

    /**
     * Get CPU information
     *
     * @return CpuInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see CpuInfoVo
     */
    fun cpu(): CpuInfoVo

    /**
     * Get storage information
     *
     * @return StorageInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see StorageInfoVo
     */
    fun storage(): StorageInfoVo

    /**
     * Get the history of online users information
     *
     * @param onlineInfoGetParam Get online information parameters
     * @return OnlineInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see OnlineInfoGetParam
     * @see OnlineInfoVo
     */
    fun online(onlineInfoGetParam: OnlineInfoGetParam?): OnlineInfoVo

    /**
     * Get the history of active information
     *
     * @param activeInfoGetParam Get active information parameters
     * @return ActiveInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ActiveInfoGetParam
     * @see ActiveInfoVo
     */
    fun active(activeInfoGetParam: ActiveInfoGetParam?): ActiveInfoVo
}
