package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.param.system.api.ApiDocGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiDocVo

/**
 * User-facing API documentation service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiDocService {
    /**
     * Get a page of the API documentation catalog
     *
     * Each record carries only the interfaces the calling user may call.
     *
     * @param apiDocGetParam Get API doc parameters (paging plus an optional plugin-name search)
     * @return PageVo<ApiDocVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPage(apiDocGetParam: ApiDocGetParam?): PageVo<ApiDocVo>

    /**
     * Get the API documentation of one plugin, including its embedded OpenAPI fragment
     *
     * Refuses a plugin the calling user has no callable interface in.
     *
     * @param pluginId Plugin ID
     * @return ApiDocVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getDetail(pluginId: String): ApiDocVo
}
