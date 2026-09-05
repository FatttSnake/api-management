package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.api.ApiPluginDatasource
import top.fatweb.apimanagement.mapper.api.ApiPluginDatasourceMapper
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiPluginDatasourceService
import top.fatweb.apimanagement.util.PluginCryptoUtil
import javax.sql.DataSource

/**
 * Plugin datasource service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ApiPluginDatasourceMapper
 * @see ApiPluginDatasource
 * @see IApiPluginDatasourceService
 */
@Service
@DS("master")
class ApiPluginDatasourceServiceImpl(
    private val serverProperties: ServerProperties
) : ServiceImpl<ApiPluginDatasourceMapper, ApiPluginDatasource>(), IApiPluginDatasourceService {
    override fun getByPluginId(pluginId: String): ApiPluginDatasource? =
        getOne(KtQueryWrapper(ApiPluginDatasource()).eq(ApiPluginDatasource::pluginId, pluginId))

    override fun buildIfConfigured(pluginId: String): DataSource? {
        val config = getByPluginId(pluginId) ?: return null
        val url = config.url?.takeIf { it.isNotBlank() } ?: return null
        val builder = DataSourceBuilder.create().url(url)
        return when (config.dbType?.uppercase()) {
            "SQLITE" -> builder.driverClassName("org.sqlite.JDBC").build()
            else -> {
                val password = config.password?.takeIf { it.isNotBlank() }
                    ?.let { PluginCryptoUtil.decrypt(serverProperties.security.tokenSecret, it) } ?: ""
                builder
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .username(config.username ?: "")
                    .password(password)
                    .build()
            }
        }
    }
}
