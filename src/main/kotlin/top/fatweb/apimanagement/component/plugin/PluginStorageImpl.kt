package top.fatweb.apimanagement.component.plugin

import org.slf4j.LoggerFactory
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.sdk.plugin.PluginStorage
import top.fatweb.apimanagement.service.system.IStorageBlobService
import java.time.Duration

/**
 * Plugin storage implement
 *
 * Binds the sanctioned storage channel of one plugin. The plugin ID comes from the
 * constructor and from nowhere else, so no method signature exists by which a plugin
 * could name another plugin's namespace; the SDK takes a path relative to the
 * plugin's own namespace and this class composes the fully qualified key.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 * @see IStorageBlobService
 * @see PluginStorage
 */
class PluginStorageImpl(
    private val pluginId: String,
    private val serverProperties: ServerProperties,
    private val fileStorageProvider: FileStorageProvider,
    private val storageBlobService: IStorageBlobService
) : PluginStorage {
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
     * The request is clamped before any time arithmetic, so an over-long value cannot
     * overflow. A non-positive request is rejected rather than clamped, because a link
     * that never works is never what a caller means.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Duration
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
