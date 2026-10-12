package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.SysLog
import top.fatweb.apimanagement.param.system.SysLogGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.SysLogVo

/**
 * System log service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ISysLogService : IService<SysLog> {
    /**
     * Get system log in page
     *
     * @param sysLogGetParam Get system log parameters
     * @return IPage<SysLog> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPage(sysLogGetParam: SysLogGetParam?): PageVo<SysLogVo>
}
