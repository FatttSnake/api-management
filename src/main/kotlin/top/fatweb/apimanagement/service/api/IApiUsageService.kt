package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiUsage
import top.fatweb.apimanagement.param.system.apiUsage.ApiUsageGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiUsageVo

/**
 * API usage service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiUsage
 */
interface IApiUsageService : IService<ApiUsage> {
    /**
     * Get API usage in page
     *
     * @param managed Whether the caller is authorized to query other users' usage
     * @param apiUsageGetParam Get API usage parameters
     * @return PageVo<ApiUsageVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUsageGetParam
     * @see PageVo
     * @see ApiUsageVo
     */
    fun getPage(managed: Boolean, apiUsageGetParam: ApiUsageGetParam?): PageVo<ApiUsageVo>

    /**
     * Save API usage record
     *
     * @param apiUsage API usage record
     * @return ID of saved record
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUsage
     */
    fun saveUsage(apiUsage: ApiUsage): Long
}
