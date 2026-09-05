package top.fatweb.apimanagement.service.api

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.api.ApiPluginDatasource
import javax.sql.DataSource

/**
 * Plugin datasource service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see ApiPluginDatasource
 */
interface IApiPluginDatasourceService : IService<ApiPluginDatasource> {
    /**
     * Get the datasource configuration of a plugin
     *
     * @param pluginId Plugin ID
     * @return ApiPluginDatasource object, or null when not configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginDatasource
     */
    fun getByPluginId(pluginId: String): ApiPluginDatasource?

    /**
     * Build the plugin's isolated DataSource from its configuration, or null when
     * the administrator has not configured one for this plugin
     *
     * @param pluginId Plugin ID
     * @return DataSource object, or null when not configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see DataSource
     */
    fun buildIfConfigured(pluginId: String): DataSource?
}
