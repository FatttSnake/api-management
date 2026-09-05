package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.param.system.apiAudit.ApiAuditGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiAuditVo

/**
 * API audit service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiAuditService {
    /**
     * Get API audit in page
     *
     * @param apiAuditGetParam Get API audit parameters
     * @return PageVo<ApiAuditVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiAuditGetParam
     * @see PageVo
     * @see ApiAuditVo
     */
    fun getPage(apiAuditGetParam: ApiAuditGetParam?): PageVo<ApiAuditVo>
}
