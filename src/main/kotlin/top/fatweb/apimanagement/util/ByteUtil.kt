package top.fatweb.apimanagement.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.math.floor

/**
 * Byte util
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object ByteUtil {
    private const val BASE = 1024

    /**
     * Byte unit
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    enum class ByteUnit {
        /**
         * Byte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        B,

        /**
         * Kibibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        KiB,

        /**
         * Mebibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        MiB,

        /**
         * Gibibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        GiB,

        /**
         * Tebibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        TiB,

        /**
         * Pebibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        PiB,

        /**
         * Exbibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        EiB,

        /**
         * Zebibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        ZiB,

        /**
         * Yobibyte
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        YiB
    }

    /**
     * Format byte size
     *
     * @param byteSize ByteSize
     * @return Result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun formatByteSize(byteSize: Long): String {
        if (byteSize <= -1) {
            return byteSize.toString()
        }

        var size = byteSize.toDouble()
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.B)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.KiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.MiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.GiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.TiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.PiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.EiB)
        }

        size /= BASE
        if (floor(size / BASE) <= 0) {
            return format(size, ByteUnit.ZiB)
        }

        size /= BASE
        return format(size, ByteUnit.YiB)
    }

    private fun format(size: Double, unit: ByteUnit): String {
        val precision = if (size * 1000 % 10 > 0) {
            3
        } else if (size * 100 % 10 > 0) {
            2
        } else if (size * 10 % 10 > 0) {
            1
        } else {
            0
        }

        val formatStr = "%.${precision}f"

        return String.format(formatStr, size) + unit.name
    }
}

/**
 * Computes the SHA-256 key of this [ByteArray]
 *
 * @return A new [ByteArray] containing the SHA-256 hash (32 bytes)
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ByteArray
 */
fun ByteArray.sha256(): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(this)

/**
 * Computes the SHA-256 hash of this [ByteArray] and returns it as a hexadecimal string
 *
 * @return A [String] representing the SHA-256 hash in lowercase hexadecimal format (64 characters)
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ByteArray
 */
fun ByteArray.sha256HexString(): String =
    this.sha256().toHexString()

/**
 * Compresses the data in this [ByteArray] using the specified output stream factory
 *
 * The factory function should wrap the given [OutputStream] with a compressing output stream
 * (e.g., [io.airlift.compress.v3.zstd.ZstdOutputStream], [java.util.zip.GZIPOutputStream]). The entire content of this byte array
 * is written to the compressing stream and the compressed result is returned.
 *
 * @param streamFactory A function that wraps an [OutputStream] with compression logic
 * @return A new [ByteArray] containing the compressed data
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ByteArray
 * @see OutputStream
 */
fun ByteArray.compress(streamFactory: (OutputStream) -> OutputStream): ByteArray {
    ByteArrayOutputStream().use { output ->
        streamFactory(output).use { compressed ->
            compressed.write(this)
        }
        return output.toByteArray()
    }
}

/**
 * Decompresses the data in this [ByteArray] using the specified input stream factory
 *
 * The factory function should wrap the given [InputStream] with a decompressing input stream
 * (e.g., [io.airlift.compress.v3.zstd.ZstdInputStream], [java.util.zip.GZIPInputStream]). The entire content of the decompressing
 * stream is read and returned as a byte array.
 *
 * @param streamFactory A function that wraps an [InputStream] with decompression logic
 * @return A new [ByteArray] containing the decompressed data
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ByteArray
 * @see InputStream
 */
fun ByteArray.decompress(streamFactory: (InputStream) -> InputStream): ByteArray {
    ByteArrayInputStream(this).use { input ->
        streamFactory(input).use { compressed ->
            ByteArrayOutputStream().use { output ->
                compressed.copyTo(output)
                return output.toByteArray()
            }
        }
    }
}

/**
 * Saves this [ByteArray] to a file at the specified path, optionally applying compression
 *
 * The file is constructed from the [base] directory and [subpaths] components. The write operation
 * is performed atomically when possible: data is first written to a temporary file, then moved to
 * the target location. If the underlying file system does not support atomic moves, a non-atomic
 * move is used as a fallback.
 *
 * If [compressStreamFactory] is provided, the data is compressed before being written using the
 * specified compression stream factory (e.g., `{ GZIPOutputStream(it) }`).
 *
 * @param base The base directory path
 * @param subpaths Additional path components to append to [base]
 * @param compressStreamFactory Optional factory for compressing the data before writing.
 *        If `null`, the raw bytes are written without compression
 * @return The [Path] to the saved file
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ByteArray
 * @see OutputStream
 * @see Path
 */
fun ByteArray.saveToFile(
    base: String,
    vararg subpaths: String,
    compressStreamFactory: ((OutputStream) -> OutputStream)? = null
): Path =
    Path(base = base, *subpaths).apply {
        parent?.createDirectories()

        val tempPath = Path("data", "temp", "${UUID.randomUUID()}.tmp")
        tempPath.parent.createDirectories()

        try {
            tempPath.writeBytes(
                compressStreamFactory
                    ?.let { this@saveToFile.compress(it) }
                    ?: this@saveToFile
            )
            try {
                Files.move(tempPath, this, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, this, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            runCatching {
                Files.deleteIfExists(tempPath)
            }
            throw e
        }
    }

/**
 * Safely saves this [ByteArray] to a file, wrapping the result in a [Result]
 *
 * This is a safe wrapper around [saveToFile] that catches exceptions and returns them
 * as a [Result.failure] instead of throwing.
 *
 * @param base The base directory path
 * @param subpaths Additional path components to append to [base]
 * @param compressStreamFactory Optional factory for compressing the data before writing.
 *        If `null`, the raw bytes are written without compression
 * @return A [Result] containing the [Path] of the saved file, or a failure if an error occurred
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see saveToFile
 * @see ByteArray
 * @see OutputStream
 * @see Result
 * @see Path
 */
fun ByteArray.saveToFileSafe(
    base: String,
    vararg subpaths: String,
    compressStreamFactory: ((OutputStream) -> OutputStream)? = null
): Result<Path> = runCatching {
    this.saveToFile(base = base, *subpaths, compressStreamFactory = compressStreamFactory)
}

/**
 * Reads the contents of the file at this [Path] as a [ByteArray], optionally applying decompression
 *
 * If [decompressStreamFactory] is provided, the raw bytes read from the file are decompressed
 * using the specified decompression stream factory (e.g., `{ GZIPInputStream(it) }`).
 *
 * @param decompressStreamFactory Optional factory for decompressing the data after reading.
 *        If `null`, the raw bytes are returned without decompression.
 * @return A [ByteArray] containing the (possibly decompressed) file contents
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Path
 * @see InputStream
 * @see ByteArray
 */
fun Path.readFile(
    decompressStreamFactory: ((InputStream) -> InputStream)? = null
): ByteArray =
    decompressStreamFactory
        ?.let {
            this.readBytes().decompress(it)
        }
        ?: this.readBytes()

/**
 * Safely reads the contents of the file at this [Path] as a [ByteArray], wrapping the result in a [Result]
 *
 * This is a safe wrapper around [readFile] that catches exceptions and returns them
 * as a [Result.failure] instead of throwing.
 *
 * @param decompressStreamFactory Optional factory for decompressing the data after reading
 * @return A [Result] containing the file contents as a [ByteArray], or a failure if an error occurred
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see readFile
 * @see Path
 * @see InputStream
 * @see Result
 * @see ByteArray
 */
fun Path.readFileSafe(
    decompressStreamFactory: ((InputStream) -> InputStream)? = null
): Result<ByteArray> = runCatching {
    this.readFile(decompressStreamFactory)
}
