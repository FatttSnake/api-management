package top.fatweb.apimanagement.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import top.fatweb.apimanagement.component.storage.FileStorageMode
import top.fatweb.apimanagement.component.storage.S3PathStyle

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
         * File storage root path
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:NotBlank val root: String = "data/objects",
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
        @field:NotNull val prefix: String = ""
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
}
