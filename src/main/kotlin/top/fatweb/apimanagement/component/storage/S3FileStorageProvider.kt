package top.fatweb.apimanagement.component.storage

import io.airlift.compress.v3.zstd.ZstdInputStream
import io.airlift.compress.v3.zstd.ZstdOutputStream
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.HttpStatusCode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.compress
import top.fatweb.apimanagement.util.decompress
import top.fatweb.apimanagement.util.sha256HexString
import java.net.URI

/**
 * S3 file storage provider
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see FileStorageProvider
 */
@Component
@ConditionalOnProperty(name = ["app.storage.mode"], havingValue = "s3")
class S3FileStorageProvider(
    private val serverProperties: ServerProperties
) : FileStorageProvider {
    private val s3 = S3Client
        .builder()
        .endpointOverride(URI(serverProperties.storage.s3!!.endpoint))
        .region(Region.of(serverProperties.storage.s3!!.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    serverProperties.storage.s3!!.accessKey,
                    serverProperties.storage.s3!!.secretKey
                )
            )
        )
        .forcePathStyle(serverProperties.storage.s3!!.pathStyle == S3PathStyle.Path)
        .build()

    private fun String.splitFileName() =
        Pair(
            substring(0, 2),
            substring(2)
        )

    private fun String.resolveS3Key(): String {
        val (dir, fileName) = splitFileName()
        val prefix =
            if (serverProperties.storage.s3!!.prefix.isEmpty()) "" else serverProperties.storage.s3!!.prefix.removeSuffix(
                "/"
            ) + "/"

        return "${prefix}objects/${dir}/${fileName}"
    }

    override fun save(content: ByteArray): String {
        val key = content.sha256HexString()
        if (exists(key)) {
            return key
        }

        val putObjectRequest = PutObjectRequest
            .builder()
            .bucket(serverProperties.storage.s3!!.bucket)
            .key(key.resolveS3Key())
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
                .bucket(serverProperties.storage.s3!!.bucket)
                .key(key.resolveS3Key())
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
                .bucket(serverProperties.storage.s3!!.bucket)
                .key(key.resolveS3Key())
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
        val deleteObjectRequest = DeleteObjectRequest
            .builder()
            .bucket(serverProperties.storage.s3!!.bucket)
            .key(key.resolveS3Key())
            .build()
        s3.deleteObject(deleteObjectRequest)

        return true
    }

    override fun size(key: String): Long? =
        try {
            val headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(serverProperties.storage.s3!!.bucket)
                .key(key.resolveS3Key())
                .build()

            s3.headObject(headObjectRequest).contentLength()
        } catch (e: S3Exception) {
            if (e.statusCode() == HttpStatusCode.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
}
