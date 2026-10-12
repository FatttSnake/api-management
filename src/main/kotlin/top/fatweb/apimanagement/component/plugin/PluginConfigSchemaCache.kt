package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component

/**
 * Plugin config schema cache
 *
 * Holds the parsed schema of every installed plugin, and says which of a plugin's keys are
 * secrets - and therefore which stored values are ciphertext. The set is replaced whole rather
 * than emptied and refilled.
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
     * A plugin whose schema is absent declares nothing.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
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
     */
    fun get(pluginId: String): PluginConfigSchema? = schemas[pluginId]

    /**
     * Publish the parsed config schema of one plugin ahead of a full refresh
     *
     * A mount publishes the schema before it runs the plugin's `onStart`, so a hook that reads
     * a setting already sees the declared default.
     *
     * @param pluginId Plugin ID
     * @param schema Parsed schema, or null when the plugin declares no configuration
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun put(pluginId: String, schema: PluginConfigSchema?) {
        val updated = mutableMapOf<String, PluginConfigSchema>()
        updated.putAll(schemas)
        if (schema != null) {
            updated[pluginId] = schema
        } else {
            updated.remove(pluginId)
        }

        schemas = updated
    }

    /**
     * Replace the parsed config schema of every plugin
     *
     * @param schemas Parsed schemas, by plugin ID; a null schema forgets the plugin, which
     *        is what a plugin declaring no configuration and one whose schema no longer
     *        parses both look like
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun replaceAll(schemas: Map<String, PluginConfigSchema?>) {
        val parsed = mutableMapOf<String, PluginConfigSchema>()
        schemas.forEach { (pluginId, schema) -> if (schema != null) parsed[pluginId] = schema }

        this.schemas = parsed
    }
}
