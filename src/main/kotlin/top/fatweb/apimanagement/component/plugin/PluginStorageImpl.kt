package top.fatweb.apimanagement.component.plugin

import org.slf4j.LoggerFactory
import top.fatweb.apimanagement.api.v1.Storage
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.IStorageBlobService
import java.time.Duration

/**
 * Plugin storage implement
 *
 * The plugin ID comes from the constructor only, and SDK file paths are relative to the
 * plugin's own namespace.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginStorageImpl(
    private val pluginId: String,
    private val serverProperties: ServerProperties,
    private val fileStorageProvider: FileStorageProvider,
    private val storageBlobService: IStorageBlobService
) : Storage {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun saveContent(content: ByteArray): String =
        storageBlobService.saveFile(content)

    override fun loadContent(key: String): ByteArray? =
        storageBlobService.loadFile(key)

    override fun existsContent(key: String): Boolean =
        storageBlobService.existsFile(key)

    override fun deleteContent(key: String) {
        storageBlobService.removeFile(key)
    }

    override fun saveFile(path: String, content: ByteArray) =
        fileStorageProvider.saveAt(StorageKeyUtil.pluginKey(pluginId, path), content)

    override fun loadFile(path: String): ByteArray? =
        fileStorageProvider.loadAt(StorageKeyUtil.pluginKey(pluginId, path))

    override fun existsFile(path: String): Boolean =
        fileStorageProvider.existsAt(StorageKeyUtil.pluginKey(pluginId, path))

    override fun deleteFile(path: String) {
        fileStorageProvider.deleteAt(StorageKeyUtil.pluginKey(pluginId, path))
    }

    override fun fileExternalUrl(path: String, ttl: Duration?): String? =
        fileStorageProvider.externalUrlAt(
            StorageKeyUtil.pluginKey(pluginId, path),
            StorageKeyUtil.pluginReference(pluginId, path),
            resolveTtl(ttl)
        )

    /**
     * Resolve the requested validity against the configured default and maximum
     *
     * A non-positive request is rejected; an over-long one is clamped to the configured maximum.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun resolveTtl(ttl: Duration?): Duration {
        val requested = ttl ?: serverProperties.storage.externalUrlDefaultDuration()
        require(!requested.isZero && !requested.isNegative) { "External URL ttl must be positive, got $requested" }

        val maximum = serverProperties.storage.externalUrlMaxDuration()

        return if (requested > maximum) {
            logger.debug("Clamping external storage URL ttl {} to the configured maximum {}", requested, maximum)

            maximum
        } else {
            requested
        }
    }
}
