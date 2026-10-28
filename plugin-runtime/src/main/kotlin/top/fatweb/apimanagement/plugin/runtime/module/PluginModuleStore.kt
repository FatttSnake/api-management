package top.fatweb.apimanagement.plugin.runtime.module

import java.nio.file.Path
import java.time.Duration

/**
 * Content-addressed store for plugin dependency modules.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginModuleStore {
    /**
     * Checks whether a module is installed.
     *
     * @param sha256 module digest
     * @return true when present
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun contains(sha256: String): Boolean

    /**
     * Returns the module jar path.
     *
     * @param sha256 module digest
     * @return jar path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun path(sha256: String): Path

    /**
     * Installs a module atomically.
     *
     * @param expectedSha256 expected digest
     * @param bytes module bytes
     * @return installed jar path
     * @throws PluginModuleStoreException when the bytes do not match or the store is full
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun install(expectedSha256: String, bytes: ByteArray): Path

    /**
     * Verifies a stored module.
     *
     * @param sha256 module digest
     * @return true when present and intact
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun verify(sha256: String): Boolean

    /**
     * Deletes a module.
     *
     * @param sha256 module digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(sha256: String)

    /**
     * Lists installed module digests.
     *
     * @return digests
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun list(): List<String>

    /**
     * Deletes unreferenced modules past the grace period.
     *
     * @param referenced digests still referenced by installed plugins
     * @param grace how long an unreferenced module is retained
     * @return number of modules deleted
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun gc(referenced: Set<String>, grace: Duration = Duration.ZERO): Int
}

/**
 * Module store failure.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleStoreException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
