package top.fatweb.apimanagement.component.plugin

import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.sdk.plugin.PluginContext
import top.fatweb.apimanagement.sdk.plugin.PluginInterfaceInfo
import top.fatweb.apimanagement.sdk.plugin.PluginStorage
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
 * [datasources] and [storage].
 *
 * Settings come in two flavours that share one read method but not one owner. A key
 * declared by the plugin's own config schema is administrator-owned: the value is
 * resolved from the settings table, falls back to the schema default, and never comes
 * from the plugin itself. Any other key is the plugin's own runtime state, written
 * through [saveSetting] exactly as before.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see DataSource
 * @see PluginStorage
 * @see IApiAccountService
 * @see IApiPluginSettingService
 * @see PluginConfigSchemaCache
 * @see PluginSettingReader
 * @see ApiInterface
 * @see PluginContext
 */
class PluginContextImpl(
    override val pluginId: String,
    override val datasources: Map<String, DataSource>,
    override val storage: PluginStorage,
    private val apiAccountService: IApiAccountService,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val configSchemaCache: PluginConfigSchemaCache,
    private val pluginSettingReader: PluginSettingReader,
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

    override fun getSetting(key: String): String? = pluginSettingReader.resolve(pluginId, key)

    override fun saveSetting(key: String, value: String) {
        require(configSchemaCache.get(pluginId)?.isDeclared(key) != true) {
            "Setting '$key' is declared by the plugin config schema and can only be changed by an administrator"
        }
        // Both kinds of key land in the same column, so a key the plugin names for itself is
        // held to the limit an administrator's key is - see PluginConfigSchemaUtil
        require(key.length <= PluginConfigSchemaUtil.MAX_KEY_LENGTH) {
            "Setting key '$key' is longer than ${PluginConfigSchemaUtil.MAX_KEY_LENGTH} characters"
        }

        apiPluginSettingService.set(pluginId, key, value)
    }
}
