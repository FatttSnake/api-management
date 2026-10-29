package top.fatweb.apimanagement.plugin.runtime.module

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/**
 * File-system module store rooted at `data/plugin-modules`.
 *
 * @param root storage directory
 * @param maxModuleBytes maximum size of one module
 * @param maxModules maximum number of installed modules
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class FileSystemPluginModuleStore(
    private val root: Path,
    private val maxModuleBytes: Long = DEFAULT_MAX_MODULE_BYTES,
    private val maxModules: Int = DEFAULT_MAX_MODULES,
) : PluginModuleStore {
    init {
        Files.createDirectories(root)
    }

    /**
     * Checks whether a module is installed.
     *
     * @param sha256 module digest
     * @return true when present
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun contains(sha256: String): Boolean = Files.isRegularFile(path(sha256))

    /**
     * Returns the module jar path.
     *
     * @param sha256 module digest
     * @return jar path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun path(sha256: String): Path {
        requireDigest(sha256)
        return root.resolve("$sha256.jar")
    }

    /**
     * Installs a module atomically.
     *
     * @param expectedSha256 expected digest
     * @param bytes module bytes
     * @return installed jar path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    override fun install(expectedSha256: String, bytes: ByteArray): Path {
        requireDigest(expectedSha256)
        if (bytes.size.toLong() > maxModuleBytes) {
            throw PluginModuleStoreException(
                "Module is ${bytes.size} bytes, limit is $maxModuleBytes",
            )
        }
        val actual = sha256(bytes)
        if (actual != expectedSha256) {
            throw PluginModuleStoreException(
                "Module digest mismatch: expected $expectedSha256, got $actual",
            )
        }

        val target = path(expectedSha256)
        if (Files.exists(target)) {
            if (!verify(expectedSha256)) {
                throw PluginModuleStoreException("Stored module $expectedSha256 is corrupted")
            }
            return target
        }
        if (list().size >= maxModules) {
            throw PluginModuleStoreException("Module store is full: $maxModules modules")
        }

        val temporary = Files.createTempFile(root, ".install-", ".tmp")
        try {
            Files.write(temporary, bytes)
            moveAtomically(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return target
    }

    /**
     * Verifies a stored module.
     *
     * @param sha256 module digest
     * @return true when present and intact
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun verify(sha256: String): Boolean {
        val file = path(sha256)
        if (!Files.isRegularFile(file)) {
            return false
        }
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex() == sha256
    }

    /**
     * Deletes a module.
     *
     * @param sha256 module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    override fun delete(sha256: String) {
        Files.deleteIfExists(path(sha256))
    }

    /**
     * Lists installed module digests.
     *
     * @return digests
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    override fun list(): List<String> {
        if (!Files.isDirectory(root)) {
            return emptyList()
        }
        return Files.list(root).use { stream ->
            stream
                .map { it.fileName.toString() }
                .filter { it.endsWith(".jar") }
                .map { it.removeSuffix(".jar") }
                .filter { DIGEST.matches(it) }
                .sorted()
                .toList()
        }
    }

    /**
     * Deletes unreferenced modules past the grace period.
     *
     * @param referenced digests still referenced by installed plugins
     * @param grace how long an unreferenced module is retained
     * @return number of modules deleted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Synchronized
    override fun gc(referenced: Set<String>, grace: Duration): Int {
        if (!Files.isDirectory(root)) {
            return 0
        }
        val cutoff = Instant.now().minus(grace)
        var deleted = 0
        list()
            .filterNot { it in referenced }
            .forEach { digest ->
                val file = path(digest)
                val modified = Files.getLastModifiedTime(file).toInstant()
                if (!modified.isAfter(cutoff)) {
                    Files.deleteIfExists(file)
                    deleted++
                }
            }
        return deleted
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun requireDigest(value: String) {
        if (!DIGEST.matches(value)) {
            throw PluginModuleStoreException("Invalid module digest: $value")
        }
    }

    companion object {
        /**
         * Default single module size limit (64 MiB).
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val DEFAULT_MAX_MODULE_BYTES: Long = 64L * 1024 * 1024

        /**
         * Default module count limit.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val DEFAULT_MAX_MODULES: Int = 4096

        private val DIGEST = Regex("^[0-9a-f]{64}$")

        /**
         * Computes the SHA-256 digest of bytes.
         *
         * @param bytes bytes
         * @return lowercase hex digest
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
