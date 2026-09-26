package top.fatweb.apimanagement.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.springframework.validation.annotation.Validated
import top.fatweb.apimanagement.component.storage.FileStorageMode
import top.fatweb.apimanagement.component.storage.S3PathStyle
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * File storage properties
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Validated
data class StorageProperties(
    /**
     * File storage mode
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see FileStorageMode
     */
    val mode: FileStorageMode = FileStorageMode.Local,

    /**
     * Local storage properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalStorageProperties
     */
    @field:Valid val local: LocalStorageProperties = LocalStorageProperties(),

    /**
     * Directory where plugin jars are materialized for class loading
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val pluginDir: String = "data/plugins",

    /**
     * Directory holding the SQLite databases of the plugins that declare one
     *
     * Each plugin gets a directory named after its ID and each of its datasources a file
     * inside it, so nothing an administrator submits ever reaches this path
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:NotBlank val pluginDatasourceDir: String = "data/db/plugin",

    /**
     * Most connections one plugin datasource may hold open
     *
     * Every datasource a plugin declares is its own pool, so this is what decides how
     * many connections a plugin can add to the database it points at. Kept next to
     * [pluginDatasourceDir] because both are settings about the datasources the gateway
     * supplies, and neither is about where files are stored.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Min(1) val pluginDatasourcePoolSize: Int = 5,

    /**
     * Public base URL used to build external storage links
     *
     * When unset, the URL of the current request is used, which is correct for a
     * direct deployment but wrong behind a reverse proxy - set it explicitly in
     * production
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Pattern(
        regexp = "^https?://\\S+$",
        message = "Public base URL must be an absolute http(s) URL"
    ) val publicBaseUrl: String? = null,

    /**
     * Default life of an external storage link
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val externalUrlDefaultTtl: Long = 1L,

    /**
     * Life unit of external storage links
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimeUnit
     */
    val externalUrlTtlUnit: TimeUnit = TimeUnit.HOURS,

    /**
     * Maximum life of an external storage link, a longer request is clamped down
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val externalUrlMaxTtl: Long = 168L,

    /**
     * S3 storage properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see S3StorageProperties
     */
    @field:Valid val s3: S3StorageProperties? = null
) {
    /**
     * Local storage properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class LocalStorageProperties(
        /**
         * File storage root path, the 'objects/' and 'files/' segments are appended
         * automatically
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val root: String = "data",
    )

    data class S3StorageProperties(
        /**
         * S3 endpoint
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val endpoint: String = "",

        /**
         * S3 access key
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val accessKey: String = "",

        /**
         * S3 secret key
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val secretKey: String = "",

        /**
         * S3 region
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val region: String = "",

        /**
         * S3 path style
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see S3PathStyle
         */
        @field:NotNull val pathStyle: S3PathStyle = S3PathStyle.Path,

        /**
         * S3 bucket
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val bucket: String = "",

        /**
         * S3 storage path prefix
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotNull val prefix: String = "",

        /**
         * Public S3 endpoint used to build presigned URLs, falls back to [endpoint]
         *
         * Set it when the internal endpoint is not reachable from a browser, e.g. a
         * container service name or a private network address
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:Pattern(
            regexp = "^https?://\\S+$",
            message = "Public S3 endpoint must be an absolute http(s) URL"
        ) val publicEndpoint: String? = null,

        /**
         * Region used to sign presigned URLs, falls back to [region]
         *
         * SigV4 requires a real signing region, so the provider substitutes
         * 'us-east-1' for the 'auto' that object stores such as MinIO accept
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val signingRegion: String? = null
    )

    /**
     * Check s3 properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @AssertTrue(message = "S3 configuration must be added")
    fun isS3(): Boolean =
        mode == FileStorageMode.Local || s3 !== null

    /**
     * Check external link ttl properties
     *
     * A default above the maximum would be clamped on every single call, which is
     * never what an operator means
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @AssertTrue(message = "External storage link default life must not exceed the maximum")
    fun isExternalUrlTtl(): Boolean =
        externalUrlDefaultTtl <= externalUrlMaxTtl

    /**
     * Get the default life of an external storage link
     *
     * @return Default life
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Duration
     */
    fun externalUrlDefaultDuration(): Duration =
        ttlToDuration(externalUrlDefaultTtl)

    /**
     * Get the maximum life of an external storage link
     *
     * @return Maximum life
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Duration
     */
    fun externalUrlMaxDuration(): Duration =
        ttlToDuration(externalUrlMaxTtl)

    private fun ttlToDuration(value: Long): Duration =
        Duration.ofNanos(externalUrlTtlUnit.toNanos(value))
}
