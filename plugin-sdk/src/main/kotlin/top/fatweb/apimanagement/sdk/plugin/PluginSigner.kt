package top.fatweb.apimanagement.sdk.plugin

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.TreeMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Plugin signer
 *
 * Ed25519 signing / verification of plugin jars to prevent tampering. The
 * signature is computed over a canonical "plugin manifest": every jar entry
 * `name:SHA256` line plus the embedded public key, excluding the signature entry
 * itself (to avoid a self-referential digest).
 *
 * Jar layout:
 * - `META-INF/api-plugin.json`    descriptor
 * - `META-INF/plugin.pub.pem`     public key (SPKI PEM)
 * - `META-INF/plugin.sig`         raw Ed25519 signature bytes
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginSigner {
    /**
     * Jar entry path of the signature
     */
    const val SIGNATURE_ENTRY = "META-INF/plugin.sig"

    /**
     * Jar entry path of the public key
     */
    const val PUBLIC_KEY_ENTRY = "META-INF/plugin.pub.pem"

    /**
     * Generate an Ed25519 key pair for a plugin developer
     *
     * @return Key pair
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    /**
     * Serialize a public key to SPKI PEM
     *
     * @param publicKey Public key
     * @return PEM string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun publicKeyPem(publicKey: PublicKey): String = pem(publicKey.encoded, "PUBLIC KEY")

    /**
     * Serialize a private key to PKCS8 PEM
     *
     * @param privateKey Private key
     * @return PEM string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun privateKeyPem(privateKey: PrivateKey): String = pem(privateKey.encoded, "PRIVATE KEY")

    /**
     * Sign the plugin jar and return the signature bytes (to be written to
     * `META-INF/plugin.sig` in the final jar)
     *
     * @param jar Plugin jar path (must already contain the public key entry)
     * @param privateKeyPem Private key in PKCS8 PEM
     * @return Signature bytes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sign(jar: Path, privateKeyPem: String): ByteArray {
        val manifest = buildManifest(jar)
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(parsePrivateKey(privateKeyPem))
        signature.update(manifest)
        return signature.sign()
    }

    /**
     * Verify a jar's signature against the public key embedded in the jar itself
     *
     * @param jar Plugin jar path
     * @return true when the signature is valid and the jar is untampered
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun verify(jar: Path): Boolean =
        readPublicKey(jar)?.let { verify(jar, it) } ?: false

    /**
     * Verify a jar's signature against an embedded public key and the signature entry
     *
     * @param jar Plugin jar path
     * @param publicKeyPem Public key in SPKI PEM
     * @return true when the signature is valid and the jar is untampered
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun verify(jar: Path, publicKeyPem: String): Boolean {
        val signatureBytes = readEntryBytes(jar, SIGNATURE_ENTRY) ?: return false
        return try {
            val manifest = buildManifest(jar)
            val signature = Signature.getInstance("Ed25519")
            signature.initVerify(parsePublicKey(publicKeyPem))
            signature.update(manifest)
            signature.verify(signatureBytes)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Read the public key embedded in a plugin jar
     *
     * @param jar Plugin jar path
     * @return SPKI PEM public key, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun readPublicKey(jar: Path): String? =
        readEntryBytes(jar, PUBLIC_KEY_ENTRY)?.toString(Charsets.UTF_8)

    /**
     * Compute the stable key id of a public key (SHA-256 of the SPKI bytes, hex),
     * used to look the signer up in the gateway's trust store
     *
     * @param publicKeyPem Public key in SPKI PEM
     * @return Hex fingerprint
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun spkiFingerprint(publicKeyPem: String): String =
        sha256Hex(parsePublicKey(publicKeyPem).encoded)

    /**
     * Command-line entry for plugin builds:
     * - `genkey <dir>`         generate an Ed25519 key pair into the directory
     * - `sign <jar> <privateKeyPem>`  sign a jar and embed the signature entry
     * - `verify <jar>`         verify a jar's embedded signature
     *
     * @param args Command-line arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @JvmStatic
    fun main(args: Array<String>) {
        when (args.getOrNull(0)) {
            "genkey" -> {
                val dir = Path.of(args[1])
                Files.createDirectories(dir)
                val pair = generateKeyPair()
                Files.writeString(dir.resolve("private.pem"), privateKeyPem(pair.private))
                Files.writeString(dir.resolve("public.pem"), publicKeyPem(pair.public))
                println("Ed25519 keys written to $dir")
            }

            "sign" -> {
                val jar = Path.of(args[1])
                val privateKeyPem = Files.readString(Path.of(args[2]))
                val signature = sign(jar, privateKeyPem)
                addJarEntry(jar, SIGNATURE_ENTRY, signature)
                println("Signed ${jar.fileName}")
            }

            "verify" -> {
                val ok = verify(Path.of(args[1]))
                println(if (ok) "Signature valid" else "Signature INVALID")
                if (!ok) kotlin.system.exitProcess(1)
            }

            else -> error("Usage: genkey <dir> | sign <jar> <privateKeyPem> | verify <jar>")
        }
    }

    private fun addJarEntry(jarPath: Path, name: String, bytes: ByteArray) {
        val temp = Files.createTempFile("plugin", ".jar")
        JarOutputStream(Files.newOutputStream(temp)).use { out ->
            JarFile(jarPath.toFile()).use { source ->
                source.entries().asSequence().forEach { entry ->
                    if (entry.name == name) return@forEach
                    out.putNextEntry(JarEntry(entry.name))
                    source.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
            out.putNextEntry(JarEntry(name))
            out.write(bytes)
            out.closeEntry()
        }
        Files.move(temp, jarPath, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun buildManifest(jar: Path): ByteArray {
        val entries = TreeMap<String, String>()
        JarFile(jar.toFile()).use { jarFile ->
            jarFile.entries().asSequence().forEach { entry ->
                if (entry.isDirectory || entry.name == SIGNATURE_ENTRY) return@forEach
                val hash = jarFile.getInputStream(entry).use { sha256Hex(it.readBytes()) }
                entries[entry.name] = hash
            }
        }
        val publicKey = readEntryBytes(jar, PUBLIC_KEY_ENTRY)?.toString(Charsets.UTF_8) ?: ""
        val builder = StringBuilder()
        entries.forEach { (name, hash) -> builder.append(name).append(':').append(hash).append('\n') }
        builder.append("PUBLIC_KEY:\n").append(publicKey)
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    private fun readEntryBytes(jar: Path, name: String): ByteArray? =
        JarFile(jar.toFile()).use { jarFile ->
            jarFile.getJarEntry(name)?.let { entry ->
                jarFile.getInputStream(entry).use { it.readBytes() }
            }
        }

    private fun parsePublicKey(pem: String): PublicKey =
        KeyFactory.getInstance("Ed25519")
            .generatePublic(X509EncodedKeySpec(decodePem(pem, "PUBLIC KEY")))

    private fun parsePrivateKey(pem: String): PrivateKey =
        KeyFactory.getInstance("Ed25519")
            .generatePrivate(PKCS8EncodedKeySpec(decodePem(pem, "PRIVATE KEY")))

    private fun decodePem(pem: String, type: String): ByteArray {
        val body = pem
            .replace("-----BEGIN $type-----", "")
            .replace("-----END $type-----", "")
            .filter { !it.isWhitespace() }
        return Base64.getDecoder().decode(body)
    }

    private fun pem(bytes: ByteArray, type: String): String {
        val base64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(bytes)
        return "-----BEGIN $type-----\n$base64\n-----END $type-----\n"
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
