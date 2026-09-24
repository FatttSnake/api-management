package top.fatweb.apimanagement.component.storage

import io.airlift.compress.v3.zstd.ZstdInputStream
import io.airlift.compress.v3.zstd.ZstdOutputStream
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.HttpStatusCode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.compress
import top.fatweb.apimanagement.util.decompress
import top.fatweb.apimanagement.util.sha256HexString
import java.net.URI
import java.time.Duration

/**
 * S3 file storage provider
 *
 * Content-addressed objects are zstd-compressed under `{prefix}objects/`;
 * location-addressed objects are stored verbatim under `{prefix}files/`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 */
@Component
@ConditionalOnProperty(name = ["app.storage.mode"], havingValue = "s3")
class S3FileStorageProvider(
    serverProperties: ServerProperties
) : FileStorageProvider {
    private companion object {
        const val OBJECTS_DIR = "objects"

        /**
         * Root of the system-wide location-addressed area, a sibling of [OBJECTS_DIR]
         */
        const val FILES_DIR = "files"

        /**
         * Bound on the paginated prefix delete; an unbounded loop on a request
         * thread is worse than a partial delete plus a log line
         */
        const val MAX_PREFIX_DELETE_PAGES = 100
        const val DELETE_BATCH_SIZE = 1000
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val s3Properties = serverProperties.storage.s3!!

    private val s3 = S3Client
        .builder()
        .endpointOverride(URI(s3Properties.endpoint))
        .region(Region.of(s3Properties.region))
        .credentialsProvider(credentialsProvider())
        .forcePathStyle(s3Properties.pathStyle == S3PathStyle.Path)
        .build()

    private val keyPrefix =
        if (s3Properties.prefix.trim('/').isEmpty()) "" else "${s3Properties.prefix.trim('/')}/"

    /**
     * Region embedded in presigned URLs. SigV4 requires a real signing region, so the
     * 'auto' that object stores such as MinIO accept cannot be used verbatim.
     */
    private val presignRegion: String = run {
        s3Properties.signingRegion?.trim()?.takeIf { it.isNotEmpty() } ?: run {
            val configured = s3Properties.region.trim()
            if (configured.isEmpty() || configured.equals("auto", true)) {
                logger.warn(
                    "app.storage.s3.region='{}' cannot sign URLs, presigned URLs will use 'us-east-1'. " +
                        "Set app.storage.s3.signing-region when the object store uses another region.",
                    configured
                )

                "us-east-1"
            } else {
                configured
            }
        }
    }

    /**
     * Built lazily so a presign configuration problem cannot fail plugin install.
     *
     * It needs its own builder: `S3Presigner.builder().s3Client(...)` would inherit the
     * client's endpoint and region, and the client's `forcePathStyle` is not carried
     * over at all - the presigner rewrites a path-style key into a virtual-hosted
     * subdomain unless told otherwise.
     */
    private val presigner: S3Presigner by lazy {
        S3Presigner
            .builder()
            .endpointOverride(URI(s3Properties.publicEndpoint?.takeIf { it.isNotBlank() } ?: s3Properties.endpoint))
            .region(Region.of(presignRegion))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(
                S3Configuration
                    .builder()
                    .pathStyleAccessEnabled(s3Properties.pathStyle == S3PathStyle.Path)
                    .build()
            )
            .build()
    }

    private fun credentialsProvider() =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
        )

    private fun String.contentKey(): String {
        require(StorageKeyUtil.CONTENT_KEY_REGEX.matches(this)) { "Invalid content key: '$this'" }

        return "${keyPrefix}$OBJECTS_DIR/${substring(0, 2)}/${substring(2)}"
    }

    private fun String.locationKey(): String {
        StorageKeyUtil.validateLocationKey(this)

        return "$keyPrefix$FILES_DIR/$this"
    }

    override fun save(content: ByteArray): String {
        val key = content.sha256HexString()
        if (exists(key)) {
            return key
        }

        val putObjectRequest = PutObjectRequest
            .builder()
            .bucket(s3Properties.bucket)
            .key(key.contentKey())
            .build()
        s3.putObject(putObjectRequest, RequestBody.fromBytes(content.compress(::ZstdOutputStream)))

        return key
    }

    override fun save(content: String): String =
        save(content.toByteArray())

    override fun load(key: String): ByteArray? =
        try {
            val getObjectRequest = GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key.contentKey())
                .build()
            s3.getObject(getObjectRequest).readBytes().decompress(::ZstdInputStream)
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }

    override fun exists(key: String): Boolean =
        try {
            val headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key.contentKey())
                .build()
            s3.headObject(headObjectRequest)

            true
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                false
            } else {
                throw e
            }
        }

    override fun delete(key: String): Boolean {
        if (!exists(key)) {
            return false
        }

        val deleteObjectRequest = DeleteObjectRequest
            .builder()
            .bucket(s3Properties.bucket)
            .key(key.contentKey())
            .build()
        s3.deleteObject(deleteObjectRequest)

        return true
    }

    override fun size(key: String): Long? =
        try {
            val headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key.contentKey())
                .build()

            s3.headObject(headObjectRequest).contentLength()
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }

    override fun saveAt(key: String, content: ByteArray) {
        val putObjectRequest = PutObjectRequest
            .builder()
            .bucket(s3Properties.bucket)
            .key(key.locationKey())
            .build()
        s3.putObject(putObjectRequest, RequestBody.fromBytes(content))
    }

    override fun loadAt(key: String): ByteArray? =
        try {
            val getObjectRequest = GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key.locationKey())
                .build()
            s3.getObject(getObjectRequest).readBytes()
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }

    override fun existsAt(key: String): Boolean =
        try {
            val headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key.locationKey())
                .build()
            s3.headObject(headObjectRequest)

            true
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                false
            } else {
                throw e
            }
        }

    override fun deleteAt(key: String): Boolean {
        if (!existsAt(key)) {
            return false
        }

        val deleteObjectRequest = DeleteObjectRequest
            .builder()
            .bucket(s3Properties.bucket)
            .key(key.locationKey())
            .build()
        s3.deleteObject(deleteObjectRequest)

        return true
    }

    override fun deleteAtPrefix(key: String): Int {
        val prefix = "${key.locationKey()}/"

        var deleted = 0
        var pages = 0
        var token: String? = null

        do {
            val page = s3.listObjectsV2(
                ListObjectsV2Request
                    .builder()
                    .bucket(s3Properties.bucket)
                    .prefix(prefix)
                    .continuationToken(token)
                    .maxKeys(DELETE_BATCH_SIZE)
                    .build()
            )

            val keys = page.contents().map { ObjectIdentifier.builder().key(it.key()).build() }
            if (keys.isNotEmpty()) {
                val result = s3.deleteObjects(
                    DeleteObjectsRequest
                        .builder()
                        .bucket(s3Properties.bucket)
                        .delete(Delete.builder().objects(keys).build())
                        .build()
                )
                // S3 reports per-key failures inside an HTTP 200, so they have to be
                // read off the response rather than caught.
                result.errors().forEach { logger.warn("Failed to delete '{}': {}", it.key(), it.message()) }
                deleted += result.deleted().size
            }

            token = page.nextContinuationToken()
        } while (page.isTruncated == true && ++pages < MAX_PREFIX_DELETE_PAGES)

        if (pages >= MAX_PREFIX_DELETE_PAGES) {
            logger.warn("Stopped deleting prefix '{}' after {} pages, the rest has to be removed manually", prefix, pages)
        }

        return deleted
    }

    override fun externalUrlAt(key: String, reference: String, ttl: Duration): String? =
        // The client talks to the object store directly, so the URL is signed against
        // the stored key and the gateway's own download route is not involved at all.
        presigner.presignGetObject(
            GetObjectPresignRequest
                .builder()
                .signatureDuration(ttl)
                .getObjectRequest(
                    GetObjectRequest
                        .builder()
                        .bucket(s3Properties.bucket)
                        .key(key.locationKey())
                        .build()
                )
                .build()
        ).url().toString()
}
