package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.util.PluginCryptoUtil

/**
 * Plugin setting reader
 *
 * The one way a stored setting becomes a value: the stored value wins, the declared default
 * stands in for one never stored, and a secret is decrypted.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginSettingReader(
    private val serverProperties: ServerProperties,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val configSchemaCache: PluginConfigSchemaCache,
) {
    /**
     * Read a setting without decrypting it
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @return Stored value or the schema default; ciphertext for a secret
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolveRaw(pluginId: String, key: String): String? =
        apiPluginSettingService.get(pluginId, key) ?: configSchemaCache.get(pluginId)?.defaultOf(key)

    /**
     * Read a setting
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
        // rather than at each call site that reads a value out of the table - which is also
        // why the schema of a mounted plugin has to be in this cache before it can read
        // anything, and why PluginConfigSchemaCache swaps its contents rather than emptying
        // itself first
        return if (schema?.isSecret(key) == true) decrypt(key, stored) else stored
    }

    /**
     * Read every setting the plugin has stored
     *
     * Unlike [resolve], a secret that cannot be decrypted comes back as its ciphertext rather
     * than raising.
     *
     * @param pluginId Plugin ID
     * @return Stored values, by setting key; a key with no row is absent, and the declared
     *         default is not applied - this is what is stored, not what is read
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see resolve
     */
    fun resolveAll(pluginId: String): Map<String, String?> {
        val schema = configSchemaCache.get(pluginId)

        return apiPluginSettingService.listByPlugin(pluginId).mapValues { (key, value) ->
            if (schema?.isSecret(key) == true) decryptOrCiphertext(value) else value
        }
    }

    /**
     * Check whether a stored secret can still be read
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @return true=the stored value, if any, reads back
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isReadable(pluginId: String, key: String): Boolean = runCatching { resolve(pluginId, key) }.isSuccess

    /**
     * Decrypt a stored secret
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun decrypt(key: String, stored: String): String =
        runCatching { PluginCryptoUtil.decrypt(serverProperties.security.tokenSecret, stored) }
            .getOrElse { e ->
                throw IllegalStateException(
                    "Failed to decrypt plugin setting '$key'; the gateway token secret may have been rotated",
                    e,
                )
            }

    /**
     * Decrypt a stored secret, or keep it as it is
     *
     * @param stored Stored ciphertext
     * @return Plaintext, or the ciphertext when it cannot be decrypted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see resolveAll
     */
    private fun decryptOrCiphertext(stored: String): String =
        runCatching { PluginCryptoUtil.decrypt(serverProperties.security.tokenSecret, stored) }
            .getOrDefault(stored)
}
