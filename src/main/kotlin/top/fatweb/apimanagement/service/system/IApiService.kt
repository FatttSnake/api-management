package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.Api
import top.fatweb.apimanagement.param.system.api.ApiGetParam
import top.fatweb.apimanagement.param.system.api.ApiUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.system.ApiVo

/**
 * API service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see Api
 */
interface IApiService : IService<Api> {
    /**
     * Get API in page
     *
     * @param apiGetParam Get API parameters
     * @return PageVo<ApiVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiGetParam
     * @see PageVo
     * @see ApiVo
     */
    fun getPage(apiGetParam: ApiGetParam?): PageVo<ApiVo>

    /**
     * Update API
     *
     * @param apiUpdateParam Update API parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiUpdateParam
     */
    fun update(apiUpdateParam: ApiUpdateParam)

    /**
     * Get API by code from in-memory registry
     *
     * @param code API scoping code
     * @return Api object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Api
     */
    fun getByCode(code: String): Api?

    /**
     * Register API controllers to registry
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun registerApis()
}
