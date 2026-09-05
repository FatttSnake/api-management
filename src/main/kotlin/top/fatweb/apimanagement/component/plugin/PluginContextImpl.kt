package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.sdk.plugin.PluginContext
import top.fatweb.apimanagement.sdk.plugin.PluginInterfaceInfo
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.util.getApiKeyPrincipal
import top.fatweb.apimanagement.util.getLoginUserId
import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Plugin context implement
 *
 * Bridges a plugin to the gateway through the narrow, sanctioned data interaction
 * channel. The plugin never sees the gateway's own datasource / MyBatis mappers;
 * it reads gateway data only through these methods and writes its own data via
 * [datasource].
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginContext
 */
class PluginContextImpl(
    override val pluginId: String,
    override val datasource: DataSource?,
    private val apiAccountService: IApiAccountService,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val interfaceLookup: (String) -> ApiInterface?
) : PluginContext {
    override fun currentUserId(): Long? = getApiKeyPrincipal()?.userId ?: getLoginUserId()

    override fun currentAccessKeyId(): Long? = getApiKeyPrincipal()?.keyId

    override fun getBalance(userId: Long): BigDecimal = apiAccountService.getBalance(userId)

    override fun getInterfaceInfo(code: String): PluginInterfaceInfo? =
        interfaceLookup(code)?.let {
            PluginInterfaceInfo(
                code = it.code ?: code,
                name = it.name,
                enable = it.enable == 1,
                price = it.price,
                billingMode = it.billingMode?.code,
                needKey = it.needKey == 1,
                rateLimit = it.rateLimit,
                accessMode = it.accessMode?.code
            )
        }

    override fun getSetting(key: String): String? = apiPluginSettingService.get(pluginId, key)

    override fun saveSetting(key: String, value: String) = apiPluginSettingService.set(pluginId, key, value)
}
