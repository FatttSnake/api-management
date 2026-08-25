package top.fatweb.apimanagement.component.storage

import io.airlift.compress.v3.zstd.ZstdInputStream
import io.airlift.compress.v3.zstd.ZstdOutputStream
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.readFile
import top.fatweb.apimanagement.util.saveToFile
import top.fatweb.apimanagement.util.sha256HexString
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

/**
 * Local file storage provider
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 */
@Component
@ConditionalOnProperty(name = ["app.storage.mode"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageProvider(
    private val serverProperties: ServerProperties
) : FileStorageProvider {
    private fun String.splitFileName() =
        Pair(
            this.substring(0, 2),
            this.substring(2)
        )

    private fun String.resolvePath(): Path {
        val (dir, fileName) = this.splitFileName()

        return Path(serverProperties.storage.local.root, dir, fileName)
    }

    override fun save(content: ByteArray): String {
        val key = content.sha256HexString()
        if (this.exists(key)) {
            return key
        }

        val (dir, fileName) = key.splitFileName()

        return content
            .saveToFile(
                base = serverProperties.storage.local.root,
                dir,
                fileName,
                compressStreamFactory = ::ZstdOutputStream
            )
            .let { key }
    }

    override fun save(content: String): String =
        this.save(content.toByteArray())

    override fun load(key: String): ByteArray? {
        if (!this.exists(key)) {
            return null
        }

        return key.resolvePath().readFile(::ZstdInputStream)
    }

    override fun exists(key: String): Boolean =
        key.resolvePath().isRegularFile()

    override fun delete(key: String): Boolean =
        runCatching {
            key.resolvePath().deleteIfExists()
        }.getOrDefault(false)

    override fun size(key: String): Long? {
        if (!this.exists(key)) {
            return null
        }

        return runCatching {
            key.resolvePath().fileSize()
        }.getOrNull()
    }
}
