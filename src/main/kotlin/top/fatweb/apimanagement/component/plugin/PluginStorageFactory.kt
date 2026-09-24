package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.plugin.PluginStorage
import top.fatweb.apimanagement.service.system.IStorageBlobService

/**
 * Plugin storage factory
 *
 * Builds the storage channel of a plugin, keeping its collaborators out of the
 * plugin service constructor.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginStorageImpl
 */
@Component
class PluginStorageFactory(
    private val storageBlobService: IStorageBlobService,
    private val fileStorageProvider: FileStorageProvider,
    private val serverProperties: ServerProperties
) {
    /**
     * Create the storage channel of a plugin
     *
     * @param pluginId Plugin ID
     * @return Plugin storage
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginStorage
     */
    fun create(pluginId: String): PluginStorage =
        PluginStorageImpl(
            pluginId = pluginId,
            storageBlobService = storageBlobService,
            fileStorageProvider = fileStorageProvider,
            serverProperties = serverProperties
        )
}
