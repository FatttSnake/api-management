package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.param.system.apiReport.ApiReportGetParam
import top.fatweb.apimanagement.vo.api.ApiReportVo
import top.fatweb.apimanagement.vo.api.ApiTopVo

/**
 * API report service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiReportService {
    /**
     * Get API usage report
     *
     * @param apiReportGetParam Get API report parameters
     * @return List of ApiReportVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ApiReportVo
     */
    fun usage(apiReportGetParam: ApiReportGetParam?): List<ApiReportVo>

    /**
     * Get API cost report
     *
     * @param apiReportGetParam Get API report parameters
     * @return List of ApiReportVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ApiReportVo
     */
    fun cost(apiReportGetParam: ApiReportGetParam?): List<ApiReportVo>

    /**
     * Get API top list
     *
     * @param apiReportGetParam Get API report parameters
     * @return List of ApiTopVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ApiTopVo
     */
    fun top(apiReportGetParam: ApiReportGetParam?): List<ApiTopVo>

    /**
     * Export API usage report as CSV file, returns file hash
     *
     * @param apiReportGetParam Get API report parameters
     * @return Storage file hash
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     */
    fun export(apiReportGetParam: ApiReportGetParam?): String
}
