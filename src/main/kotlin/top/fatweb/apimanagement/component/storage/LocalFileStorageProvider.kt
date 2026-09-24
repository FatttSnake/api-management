package top.fatweb.apimanagement.component.storage

import io.airlift.compress.v3.zstd.ZstdInputStream
import io.airlift.compress.v3.zstd.ZstdOutputStream
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.readFile
import top.fatweb.apimanagement.util.saveToFile
import top.fatweb.apimanagement.util.sha256HexString
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Local file storage provider
 *
 * Content-addressed objects are zstd-compressed under `{root}/objects/`;
 * location-addressed objects are stored verbatim under `{root}/files/`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 */
@Component
@ConditionalOnProperty(name = ["app.storage.mode"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageProvider(
    serverProperties: ServerProperties,
    private val externalLinkSigner: ExternalLinkSigner,
    private val publicBaseUrlResolver: PublicBaseUrlResolver
) : FileStorageProvider {
    private companion object {
        const val OBJECTS_DIR = "objects"
        const val FILES_DIR = "files"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val storageRoot: Path =
        Path(serverProperties.storage.local.root.trimEnd('/', '\\')).toAbsolutePath().normalize()

    private fun String.splitFileName() =
        Pair(
            substring(0, 2),
            substring(2)
        )

    private fun String.contentPath(): Path {
        require(StorageKeyUtil.CONTENT_KEY_REGEX.matches(this)) { "Invalid content key: '$this'" }

        return storageRoot.resolve(OBJECTS_DIR).resolve(substring(0, 2)).resolve(substring(2))
    }

    private fun String.locationPath(): Path {
        StorageKeyUtil.validateLocationKey(this)
        val path = storageRoot.resolve(FILES_DIR).resolve(this).normalize()
        require(path.startsWith(storageRoot)) { "Location key escapes the storage root: '$this'" }

        return path
    }

    override fun save(content: ByteArray): String {
        val key = content.sha256HexString()
        if (exists(key)) {
            return key
        }

        val (dir, fileName) = key.splitFileName()

        return content
            .saveToFile(
                base = storageRoot.resolve(OBJECTS_DIR).toString(),
                dir,
                fileName,
                compressStreamFactory = ::ZstdOutputStream
            )
            .let { key }
    }

    override fun save(content: String): String =
        save(content.toByteArray())

    override fun load(key: String): ByteArray? {
        val path = key.contentPath()
        if (!path.isRegularFile()) {
            return null
        }

        return path.readFile(::ZstdInputStream)
    }

    override fun exists(key: String): Boolean =
        key.contentPath().isRegularFile()

    override fun delete(key: String): Boolean =
        runCatching {
            key.contentPath().deleteIfExists()
        }.getOrDefault(false)

    override fun size(key: String): Long? {
        val path = key.contentPath()
        if (!path.isRegularFile()) {
            return null
        }

        return runCatching {
            path.fileSize()
        }.getOrNull()
    }

    override fun saveAt(key: String, content: ByteArray) {
        StorageKeyUtil.validateLocationKey(key)

        content.saveToFile(
            base = storageRoot.resolve(FILES_DIR).toString(),
            *key.split('/').toTypedArray(),
            compressStreamFactory = null
        )
    }

    override fun loadAt(key: String): ByteArray? {
        val path = key.locationPath()
        if (!path.isRegularFile()) {
            return null
        }

        return path.readFile()
    }

    override fun existsAt(key: String): Boolean =
        key.locationPath().isRegularFile()

    override fun deleteAt(key: String): Boolean =
        key.locationPath().deleteIfExists()

    override fun deleteAtPrefix(key: String): Int {
        val base = key.locationPath()
        if (!base.isDirectory()) {
            return 0
        }

        var deleted = 0
        // walkFileTree does not follow links, so a planted symlink is unlinked rather
        // than traversed out of the storage root.
        Files.walkFileTree(
            base,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.deleteIfExists()) {
                        deleted++
                    }

                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    dir.deleteIfExists()

                    return FileVisitResult.CONTINUE
                }
            }
        )

        return deleted
    }

    override fun externalUrlAt(key: String, reference: String, ttl: Duration): String? {
        // The gateway routes on the reference and signs it, so a malformed one would
        // otherwise land straight in a URL the gateway hands out.
        StorageKeyUtil.validateLocationKey(reference)

        val base = publicBaseUrlResolver.resolve() ?: run {
            logger.debug(
                "Cannot build an external storage URL for '{}' outside a request while " +
                    "app.storage.public-base-url is unset",
                reference
            )

            return null
        }

        val expiresAt = Instant.now().plus(ttl).epochSecond
        val signature = externalLinkSigner.sign(reference, expiresAt)

        return "$base${StorageRoutes.PUBLIC_STORAGE_PATH}/$reference?e=$expiresAt&s=$signature"
    }
}
