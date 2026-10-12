package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.plugin.PluginStorage
import top.fatweb.apimanagement.service.system.IStorageBlobService

/**
 * Plugin storage factory
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginStorageFactory(
    private val serverProperties: ServerProperties,
    private val fileStorageProvider: FileStorageProvider,
    private val storageBlobService: IStorageBlobService
) {
    /**
     * Create the storage channel of a plugin
     *
     * @param pluginId Plugin ID
     * @return Plugin storage
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun create(pluginId: String): PluginStorage =
        PluginStorageImpl(
            pluginId = pluginId,
            serverProperties = serverProperties,
            fileStorageProvider = fileStorageProvider,
            storageBlobService = storageBlobService
        )
}
