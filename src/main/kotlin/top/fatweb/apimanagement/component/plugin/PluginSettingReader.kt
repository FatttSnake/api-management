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
        // rather than at each call site that reads a value out of the table - which is also
        // why the schema of a mounted plugin has to be in this cache before it can read
        // anything, and why PluginConfigSchemaCache swaps its contents rather than emptying
        // itself first
        return if (schema?.isSecret(key) == true) decrypt(key, stored) else stored
    }

    /**
     * Read every setting the plugin has stored
     *
     * The bulk form of [resolve], for the one caller that judges a whole submission rather
     * than serving one read: what a save leaves alone is read back out of these, and whether
     * a datasource key changed is the difference between them and what the submission would
     * leave behind.
     *
     * A secret comes back decrypted, but a failure keeps its ciphertext instead of raising:
     * this is the only caller that does not hand the result to anything, and a ciphertext
     * answers the two questions it is asked - whether a value is there, and whether it is the
     * one just submitted. A plugin reading a credential goes through [resolve] instead, which
     * reports the failure, because a plugin has to know its credential is unreadable.
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
     * Reported rather than raised, because the administrator is the one who has to know: a
     * secret that cannot be decrypted means the gateway's token secret was rotated after it
     * was stored, and re-entering the value is the only thing that fixes it. A key with no
     * stored row is readable - there is no ciphertext to fail on.
     *
     * @param pluginId Plugin ID
     * @param key Setting key
     * @return true=the stored value, if any, reads back
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isReadable(pluginId: String, key: String): Boolean =
        runCatching { resolve(pluginId, key) }.isSuccess

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
