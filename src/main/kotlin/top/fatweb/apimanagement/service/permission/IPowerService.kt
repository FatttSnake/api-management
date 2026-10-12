package top.fatweb.apimanagement.service.permission

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.vo.permission.PowerSetVo

/**
 * Power service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IPowerService : IService<Power> {
    /**
     * Get all power as list
     *
     * @return powerSetVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getList(): PowerSetVo
}
