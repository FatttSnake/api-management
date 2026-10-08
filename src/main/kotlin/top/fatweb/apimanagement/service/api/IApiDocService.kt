package top.fatweb.apimanagement.service.api

import top.fatweb.apimanagement.param.system.api.ApiDocGetParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiDocVo

/**
 * User-facing API documentation service interface
 *
 * Serves the callable API catalog to end users: what the signed-in user may call, with the
 * effective price, rate limit and access mode already resolved, and - on the detail call -
 * the plugin's embedded OpenAPI fragment so the console can render it as documentation.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IApiDocService {
    /**
     * Get a page of the API documentation catalog
     *
     * One record per plugin, each carrying only the interfaces the calling user may call.
     * Plugins that are switched off, or that hold no callable interface for the user, are
     * left out entirely, so a page never advertises an interface that would be refused.
     *
     * @param apiDocGetParam Get API doc parameters (paging plus an optional plugin-name search)
     * @return PageVo<ApiDocVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiDocGetParam
     * @see PageVo
     * @see ApiDocVo
     */
    fun getPage(apiDocGetParam: ApiDocGetParam?): PageVo<ApiDocVo>

    /**
     * Get the API documentation of one plugin, including its embedded OpenAPI fragment
     *
     * Refuses a plugin the calling user has no callable interface in, rather than handing
     * out the fragment of a plugin the user cannot reach.
     *
     * @param pluginId Plugin ID
     * @return ApiDocVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiDocVo
     */
    fun getDetail(pluginId: String): ApiDocVo
}
