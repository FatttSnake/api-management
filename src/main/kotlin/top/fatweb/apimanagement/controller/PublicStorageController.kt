package top.fatweb.apimanagement.controller

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import top.fatweb.apimanagement.annotation.HiddenController
import top.fatweb.apimanagement.component.storage.ExternalLinkSigner
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.component.storage.StorageKeyUtil
import top.fatweb.apimanagement.component.storage.StorageRoutes
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.exception.StorageLinkInvalidException
import top.fatweb.apimanagement.properties.ServerProperties
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Public storage controller
 *
 * Serves the file behind a login-free external link signed by [ExternalLinkSigner].
 * The route only exists in local storage mode; in S3 mode the link points straight
 * at the object store and never reaches the gateway.
 *
 * The key is rebuilt from the parsed path variables rather than read out of the
 * request, so a caller cannot present a key the gateway did not itself compose.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 * @see ExternalLinkSigner
 */
@ConditionalOnProperty(name = ["app.storage.mode"], havingValue = "local", matchIfMissing = true)
@HiddenController
class PublicStorageController(
    private val serverProperties: ServerProperties,
    private val fileStorageProvider: FileStorageProvider,
    private val externalLinkSigner: ExternalLinkSigner
) {
    private companion object {
        /**
         * Types a browser may render in place. Everything else - notably
         * `image/svg+xml`, `text/html` and `application/xhtml+xml`, which can run
         * script - is forced to download, because this route shares an origin with
         * the admin console.
         */
        val INLINE_SAFE_TYPES = setOf(
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF,
            MediaType.parseMediaType("image/webp"),
            MediaType.parseMediaType("image/bmp")
        )

        const val MAX_CACHE_SECONDS = 3600L
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Download the file behind a signed external link
     *
     * @param pluginId Plugin ID
     * @param path Path of the file inside the plugin's namespace
     * @param expiresAt Expiry of the link as epoch seconds
     * @param signature Signature of the link
     * @return File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseEntity
     * @see ByteArray
     */
    @GetMapping("${StorageRoutes.PUBLIC_STORAGE_PATH}/{pluginId}/{*path}")
    fun download(
        @PathVariable pluginId: String,
        @PathVariable path: String,
        @RequestParam("e") expiresAt: Long,
        @RequestParam("s") signature: String
    ): ResponseEntity<ByteArray> {
        // Both are composed from the parsed path variables, never read out of the
        // request, so the signature is verified over the gateway's own reference and
        // the file is looked up under the gateway's own key.
        val relativePath = path.trim('/')
        val reference = StorageKeyUtil.pluginReference(pluginId, relativePath)

        if (!externalLinkSigner.verify(reference, expiresAt, signature)) {
            logger.warn("Rejected storage link: bad signature for reference '{}'", reference)

            throw StorageLinkInvalidException()
        }

        val now = Instant.now().epochSecond
        val maxExpiry = now + serverProperties.storage.externalUrlMaxDuration().seconds
        if (expiresAt !in (now + 1)..maxExpiry) {
            logger.warn("Rejected storage link: expiry {} is out of range for reference '{}'", expiresAt, reference)

            throw StorageLinkInvalidException()
        }

        val key = StorageKeyUtil.pluginKey(pluginId, relativePath)
        val content = fileStorageProvider.loadAt(key) ?: throw NoRecordFoundException()

        val fileName = key.substringAfterLast('/')
        val mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM)
        val disposition = (if (mediaType in INLINE_SAFE_TYPES) ContentDisposition.inline() else ContentDisposition.attachment())
            .filename(fileName, StandardCharsets.UTF_8)
            .build()

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=${(expiresAt - now).coerceAtMost(MAX_CACHE_SECONDS)}")
            .contentType(mediaType)
            .contentLength(content.size.toLong())
            .body(content)
    }
}
