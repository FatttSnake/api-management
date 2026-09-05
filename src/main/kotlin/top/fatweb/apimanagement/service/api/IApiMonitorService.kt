package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.vo.api.ApiMonitorDashboardVo

/**
 * API monitor service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiMonitorService {
    /**
     * Get API monitor dashboard
     *
     * @return ApiMonitorDashboardVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiMonitorDashboardVo
     */
    fun dashboard(): ApiMonitorDashboardVo
}
