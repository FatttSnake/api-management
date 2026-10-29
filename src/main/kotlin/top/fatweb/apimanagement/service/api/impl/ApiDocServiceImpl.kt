package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.component.plugin.PluginRegistryCache
import top.fatweb.apimanagement.converter.api.toDocVo
import top.fatweb.apimanagement.converter.api.toDocVoPage
import top.fatweb.apimanagement.converter.api.toResolvedVo
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.mapper.api.ApiPluginMapper
import top.fatweb.apimanagement.param.system.api.ApiDocGetParam
import top.fatweb.apimanagement.service.api.IApiDocService
import top.fatweb.apimanagement.service.api.IApiKeyService
import top.fatweb.apimanagement.util.setPageSort
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiDocVo

/**
 * User-facing API documentation service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
@DS("master")
class ApiDocServiceImpl(
    private val objectMapper: JsonMapper,
    private val apiPluginMapper: ApiPluginMapper,
    private val pluginRegistryCache: PluginRegistryCache,
    private val apiKeyService: IApiKeyService,
) : IApiDocService {
    override fun getPage(apiDocGetParam: ApiDocGetParam?): PageVo<ApiDocVo> {
        val page = Page<ApiPlugin>(apiDocGetParam?.currentPage ?: 1, apiDocGetParam?.pageSize ?: 20)
        setPageSort(apiDocGetParam, page, OrderItem.desc("create_time"))

        val (unrestricted, codes) = apiKeyService.callableScope()
        val pluginPage =
            apiPluginMapper.selectCallablePage(
                page = page,
                searchName = apiDocGetParam?.searchName,
                unrestricted = unrestricted,
                codes = codes,
            )

        val interfacesByPlugin =
            if (pluginPage.records.isNotEmpty()) {
                apiKeyService
                    .callableApis(pluginIds = pluginPage.records.mapNotNull(ApiPlugin::pluginId))
                    .groupBy { it.pluginId ?: "" }
            } else {
                emptyMap()
            }

        return pluginPage.toDocVoPage { plugin ->
            interfacesByPlugin[plugin.pluginId].orEmpty().map { it.toResolvedVo(plugin) }
        }
    }

    override fun getDetail(pluginId: String): ApiDocVo {
        val plugin =
            pluginRegistryCache.getByPluginId(pluginId)?.takeIf { it.enable == 1 }
                ?: throw NoRecordFoundException()

        val interfaces =
            apiKeyService
                .callableApis(pluginIds = listOf(pluginId))
                .map { it.toResolvedVo(plugin) }
        if (interfaces.isEmpty()) {
            throw NoRecordFoundException()
        }

        val openapi = plugin.openapi?.let { runCatching { objectMapper.readTree(it) }.getOrNull() }

        return plugin.toDocVo(interfaces = interfaces, openapi = openapi)
    }
}
