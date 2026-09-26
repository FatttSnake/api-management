package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.util.PluginCryptoUtil

/**
 * Plugin setting reader
 *
 * The one way a stored setting is turned into a value, shared by everything that reads
 * one: a plugin through `PluginContext.getSetting`, and the gateway itself while it
 * resolves the config values a datasource is composed from. Both have to agree on all
 * three parts of it - the stored value wins, the declared default stands in for one that
 * was never stored, and a secret is decrypted - so it lives in one place rather than
 * being written out at each call site and drifting.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see IApiPluginSettingService
 * @see PluginConfigSchemaCache
 */
@Component
class PluginSettingReader(
    private val serverProperties: ServerProperties,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val configSchemaCache: PluginConfigSchemaCache
) {
    /**
     * Read a setting
     *
     * A value that was never stored is not missing: the schema's declared default is what
     * the field means in its absence, so a plugin never repeats its own defaults in code.
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @return Setting value, the schema default, or null when neither exists
     * @throws IllegalStateException when a secret cannot be decrypted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(pluginId: String, key: String): String? {
        val schema = configSchemaCache.get(pluginId)
        val stored = apiPluginSettingService.get(pluginId, key) ?: return schema?.defaultOf(key)

        // Only the schema knows which keys are secrets, so the decryption happens here
        // rather than at each call site that reads a value out of the table
        return if (schema?.isSecret(key) == true) decrypt(key, stored) else stored
    }

    /**
     * Decrypt a stored secret
     *
     * A failure here means the ciphertext was written under a different token secret,
     * which is the administrator's way of invalidating every stored credential at once;
     * the cipher's own message says nothing about that, so it is wrapped.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun decrypt(key: String, stored: String): String =
        runCatching { PluginCryptoUtil.decrypt(serverProperties.security.tokenSecret, stored) }
            .getOrElse { e ->
                throw IllegalStateException(
                    "Failed to decrypt plugin setting '$key'; the gateway token secret may have been rotated",
                    e
                )
            }
}
