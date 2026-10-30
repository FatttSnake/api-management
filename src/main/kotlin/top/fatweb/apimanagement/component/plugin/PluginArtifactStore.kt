package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.exception.PluginNotTrustedException
import top.fatweb.apimanagement.exception.PluginSignatureInvalidException
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLock
import top.fatweb.apimanagement.plugin.runtime.module.PluginModuleLockParser
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginDescriptor
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginDescriptorParser
import top.fatweb.apimanagement.plugin.signing.PluginSigner
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.api.IApiPluginTrustKeyService
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.jar.JarFile
import java.util.jar.JarInputStream

/**
 * Content-addressed storage of plugin jars and the entries read from them.
 *
 * A jar is stored under its SHA-256, so installing byte-identical bytes again reuses the file
 * the installed row already points at. Every write is staged and moved into place atomically:
 * a crash may leave a temporary file, never a truncated jar that startup would trust.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginArtifactStore(
    private val serverProperties: ServerProperties,
    private val trustKeyService: IApiPluginTrustKeyService,
    private val descriptorParser: PluginDescriptorParser = PluginDescriptorParser(),
    private val lockParser: PluginModuleLockParser = PluginModuleLockParser(),
) {
    /**
     * Stores a jar under its digest.
     *
     * @param bytes plugin jar
     * @return stored path and whether this call created the file
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun store(bytes: ByteArray): StoredJar {
        val digest = sha256(bytes)
        val path = path(digest)
        if (Files.isRegularFile(path) && Files.size(path) == bytes.size.toLong()) {
            return StoredJar(digest, path, created = false)
        }

        Files.createDirectories(path.parent)
        val temp = Files.createTempFile(path.parent, "$digest-", ".tmp")
        try {
            Files.write(temp, bytes)
            Files.move(
                temp,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            Files.deleteIfExists(temp)
        }
        return StoredJar(digest, path, created = true)
    }

    /**
     * Verifies the embedded signature against the trust store.
     *
     * @param jar plugin jar
     * @return signer key ID
     * @throws PluginSignatureInvalidException when the jar is unsigned or tampered with
     * @throws PluginNotTrustedException when the signer is not trusted or disabled
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun verify(jar: Path): String {
        val publicKey =
            PluginSigner.readPublicKey(jar)
                ?: throw PluginSignatureInvalidException()
        if (!PluginSigner.verify(jar, publicKey)) {
            throw PluginSignatureInvalidException()
        }
        val keyId = PluginSigner.spkiFingerprint(publicKey)
        val trustKey =
            trustKeyService.getByKeyId(keyId)
                ?: throw PluginNotTrustedException()
        if (trustKey.enable != 1) {
            throw PluginNotTrustedException()
        }
        return keyId
    }

    /**
     * Reads and parses the plugin descriptor.
     *
     * @param jar plugin jar
     * @return descriptor
     * @throws PluginInstallException when the descriptor is absent or malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun descriptor(jar: Path): PluginDescriptor = parseDescriptor(
        readEntry(jar, DESCRIPTOR_ENTRY)
            ?: throw PluginInstallException("Plugin jar has no $DESCRIPTOR_ENTRY"),
    )

    /**
     * Reads and parses the plugin descriptor from jar bytes that are not stored as an artifact.
     *
     * @param jarBytes plugin jar bytes
     * @return descriptor
     * @throws PluginInstallException when the descriptor is absent or malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun descriptor(jarBytes: ByteArray): PluginDescriptor = parseDescriptor(
        readEntryOrThrow(jarBytes, DESCRIPTOR_ENTRY)
            ?: throw PluginInstallException("Plugin jar has no $DESCRIPTOR_ENTRY"),
    )

    /**
     * Reads the dependency lock.
     *
     * @param jar plugin jar
     * @return parsed lock, or an empty one when the jar declares no dependencies
     * @throws PluginInstallException when the lock is malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun lock(jar: Path): PluginDependencyLock {
        val bytes = readEntry(jar, LOCK_ENTRY) ?: return PluginDependencyLock(null, PluginModuleLock())
        return PluginDependencyLock(bytes, parseLock(bytes))
    }

    /**
     * Reads the dependency lock from jar bytes that are not stored as an artifact.
     *
     * @param bytes plugin jar bytes
     * @return parsed lock, or an empty one when the jar declares no dependencies
     * @throws PluginInstallException when the lock is malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun lock(bytes: ByteArray): PluginModuleLock {
        val lockBytes = readEntryOrThrow(bytes, LOCK_ENTRY) ?: return PluginModuleLock()
        return parseLock(lockBytes)
    }

    /**
     * Reads the declared config schema.
     *
     * @param jar plugin jar
     * @return raw schema JSON, or null when the plugin declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun configSchema(jar: Path): String? = readEntry(jar, CONFIG_ENTRY)?.toString(Charsets.UTF_8)

    /**
     * Computes the SHA-256 digest of bytes as lowercase hex.
     *
     * @param bytes bytes
     * @return hex digest
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sha256(bytes: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * Resolves the stored path of a digest.
     *
     * @param digest SHA-256 digest
     * @return jar path
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun path(digest: String): Path = Path.of(serverProperties.storage.pluginDir).resolve("$digest.jar")

    private fun readEntry(jar: Path, name: String): ByteArray? = JarFile(jar.toFile()).use { file ->
        file.getJarEntry(name)?.let { entry -> file.getInputStream(entry).readBytes() }
    }

    private fun readEntry(bytes: ByteArray, name: String): ByteArray? =
        JarInputStream(ByteArrayInputStream(bytes)).use { input ->
            var entry = input.nextJarEntry
            while (entry != null) {
                if (entry.name == name) {
                    return@use input.readBytes()
                }
                entry = input.nextJarEntry
            }
            null
        }

    private fun readEntryOrThrow(bytes: ByteArray, name: String): ByteArray? = try {
        readEntry(bytes, name)
    } catch (ex: Exception) {
        throw PluginInstallException("Cannot read plugin jar: ${ex.message}", ex)
    }

    private fun parseDescriptor(bytes: ByteArray): PluginDescriptor = try {
        descriptorParser.parse(bytes)
    } catch (ex: Exception) {
        throw PluginInstallException("Invalid plugin descriptor: ${ex.message}", ex)
    }

    private fun parseLock(bytes: ByteArray): PluginModuleLock = try {
        lockParser.parse(bytes)
    } catch (ex: Exception) {
        throw PluginInstallException("Invalid dependency lock: ${ex.message}", ex)
    }

    /**
     * A stored plugin jar and whether this call created it.
     *
     * @property digest SHA-256 digest
     * @property path stored path
     * @property created whether the file was written by this call
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    class StoredJar(val digest: String, val path: Path, val created: Boolean)

    /**
     * A dependency lock and the raw bytes it was read from.
     *
     * @property bytes raw lock JSON, or null when the jar has no lock
     * @property lock parsed lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    class PluginDependencyLock(val bytes: ByteArray?, val lock: PluginModuleLock) {
        /**
         * Digest of the raw lock, or null when absent.
         *
         * @param sha256 digest function
         * @return hex digest or null
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun hash(sha256: (ByteArray) -> String): String? = bytes?.let(sha256)
    }

    private companion object {
        const val DESCRIPTOR_ENTRY = "META-INF/api-plugin.json"
        const val LOCK_ENTRY = "META-INF/api-plugin-deps.json"
        const val CONFIG_ENTRY = "META-INF/plugin-config.json"
    }
}
