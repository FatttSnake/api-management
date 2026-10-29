package top.fatweb.apimanagement.plugin.runtime.module

import top.fatweb.apimanagement.plugin.runtime.PLUGIN_JSON_MAPPER
import java.nio.charset.StandardCharsets

/**
 * Parses `META-INF/api-plugin-deps.json`.
 *
 * @param maxModules maximum number of modules accepted
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleLockParser(private val maxModules: Int = PluginModuleLock.DEFAULT_MAX_MODULES) {
    /**
     * Parses and validates a lock.
     *
     * @param bytes JSON bytes
     * @return lock
     * @throws PluginModuleLockException when the lock cannot be parsed or is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parse(bytes: ByteArray): PluginModuleLock {
        val json =
            try {
                PLUGIN_JSON_MAPPER.readValue(bytes, LockJson::class.java)
            } catch (ex: Exception) {
                throw PluginModuleLockException("Cannot parse dependency lock: ${ex.message}")
            }
        val lock =
            PluginModuleLock(
                format = json.format,
                constraints = json.constraints,
                modules =
                json.modules.map { module ->
                    LockedModule(
                        group = module.group,
                        name = module.name,
                        version = module.version,
                        sha256 = module.sha256,
                        size = module.size,
                        source = module.source,
                        dependencies = module.dependencies,
                    )
                },
            )
        lock.validate(maxModules)
        return lock
    }

    /**
     * Parses and validates a lock.
     *
     * @param text JSON text
     * @return lock
     * @throws PluginModuleLockException when the lock cannot be parsed or is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun parse(text: String): PluginModuleLock = parse(text.toByteArray(StandardCharsets.UTF_8))

    private data class LockJson(
        val format: Int = 0,
        val constraints: Map<String, String> = emptyMap(),
        val modules: List<ModuleJson> = emptyList(),
    )

    private data class ModuleJson(
        val group: String = "",
        val name: String = "",
        val version: String = "",
        val sha256: String = "",
        val size: Long = 0,
        val source: String? = null,
        val dependencies: List<String> = emptyList(),
    )
}
