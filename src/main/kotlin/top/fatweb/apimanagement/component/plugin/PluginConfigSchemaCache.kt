package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component

/**
 * Plugin config schema cache
 *
 * Holds the parsed schema of every installed plugin. The parsed form is what the
 * plugin context needs on every `getSetting` call, so parsing it per call would put a
 * JSON parse on the hot path of every plugin request.
 *
 * It is also what says which of a plugin's keys are secrets, and therefore which stored
 * values are ciphertext: a plugin whose schema is missing here reads its own credentials
 * back as the strings they are stored as. That is why the whole set is replaced at once
 * rather than emptied and refilled - a reader that arrives half way through a refresh would
 * find no schema where one belongs.
 *
 * The cache is filled by the plugin service as a side effect of its own cache refresh
 * rather than read from it lazily, which keeps the dependency one-way: the service
 * knows about this cache, this cache knows nothing about the service.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginConfigSchema
 * @see PluginSettingReader
 */
@Component
class PluginConfigSchemaCache {
    /**
     * Parsed schema of every plugin, by plugin ID
     *
     * A map that is swapped whole, so a reader holds either the set before a refresh or the
     * set after it and never a mixture of the two. A plugin whose schema is absent declares
     * nothing - which is a real state, and the reason a refresh may not be visible as one.
     */
    @Volatile
    private var schemas: Map<String, PluginConfigSchema> = emptyMap()

    /**
     * Get the parsed config schema of a plugin
     *
     * @param pluginId Plugin ID
     * @return Parsed schema, or null when the plugin declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun get(pluginId: String): PluginConfigSchema? = schemas[pluginId]

    /**
     * Replace the parsed config schema of every plugin
     *
     * @param schemas Parsed schemas, by plugin ID; a null schema forgets the plugin, which
     *        is what a plugin declaring no configuration and one whose schema no longer
     *        parses both look like
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun replaceAll(schemas: Map<String, PluginConfigSchema?>) {
        val parsed = mutableMapOf<String, PluginConfigSchema>()
        schemas.forEach { (pluginId, schema) -> if (schema != null) parsed[pluginId] = schema }

        this.schemas = parsed
    }
}
