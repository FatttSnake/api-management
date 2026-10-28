package top.fatweb.apimanagement.plugin.signing

import java.io.InputStream
import java.math.BigInteger
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
import java.security.interfaces.EdECPrivateKey
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
 * Ed25519 signing and verification of plugin jars to prevent tampering.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginSigner {
    /**
     * Jar entry path of the signature
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val SIGNATURE_ENTRY = "META-INF/plugin.sig"

    /**
     * Jar entry path of the public key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
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
     * Derive the SPKI PEM public key from a PKCS8 PEM private key
     *
     * @param privateKeyPem Private key in PKCS8 PEM
     * @return Public key in SPKI PEM
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun publicKeyPemFromPrivate(privateKeyPem: String): String =
        pem(ed25519PublicKeySpki(ed25519Seed(privateKeyPem)), "PUBLIC KEY")

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
     * Sign a plugin jar in place and embed the signature entry
     *
     * Called in-process by the Gradle plugin.
     *
     * @param jar Plugin jar path (already containing the public key entry)
     * @param privateKeyPem Private key in PKCS8 PEM
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @JvmStatic
    fun signAndEmbed(jar: Path, privateKeyPem: String) {
        addJarEntry(jar, SIGNATURE_ENTRY, sign(jar, privateKeyPem))
        println("Signed ${jar.fileName}")
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
                val hash = jarFile.getInputStream(entry).use { sha256Hex(it) }
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

    private fun ed25519Seed(privateKeyPem: String): ByteArray {
        val key = KeyFactory.getInstance("Ed25519")
            .generatePrivate(PKCS8EncodedKeySpec(decodePem(privateKeyPem, "PRIVATE KEY")))
        val edKey = key as? EdECPrivateKey
            ?: error("Unsupported Ed25519 private key: ${key.javaClass.name}")
        return edKey.bytes.orElse(null)
            ?: error("Ed25519 private key does not expose its seed")
    }

    private fun ed25519PublicKeySpki(seed: ByteArray): ByteArray {
        val scalar = ed25519Scalar(seed)
        var point = Point(BigInteger.ZERO, BigInteger.ONE)
        for (bit in scalar.bitLength() - 1 downTo 0) {
            point = ed25519Add(point, point)
            if (scalar.testBit(bit)) point = ed25519Add(point, ED25519_BASE_POINT)
        }

        val encoded = ByteArray(32) { index ->
            point.y.shiftRight(index * 8).and(BYTE_MASK).toInt().toByte()
        }
        if (point.x.testBit(0)) encoded[31] = (encoded[31].toInt() or 0x80).toByte()
        return ED25519_SPKI_PREFIX + encoded
    }

    private fun ed25519Scalar(seed: ByteArray): BigInteger {
        val half = MessageDigest.getInstance("SHA-512").digest(seed).copyOf(32)
        half[0] = (half[0].toInt() and 248).toByte()
        half[31] = ((half[31].toInt() and 127) or 64).toByte()
        return BigInteger(1, half.reversedArray())
    }

    private data class Point(val x: BigInteger, val y: BigInteger)

    private fun ed25519Add(p: Point, q: Point): Point {
        val k = ED25519_D.multiply(p.x).multiply(q.x).multiply(p.y).multiply(q.y).mod(ED25519_P)
        val x = p.x.multiply(q.y).add(q.x.multiply(p.y)).mod(ED25519_P)
            .multiply(BigInteger.ONE.add(k).mod(ED25519_P).modInverse(ED25519_P)).mod(ED25519_P)
        val y = p.y.multiply(q.y).add(p.x.multiply(q.x)).mod(ED25519_P)
            .multiply(BigInteger.ONE.subtract(k).mod(ED25519_P).modInverse(ED25519_P)).mod(ED25519_P)
        return Point(x, y)
    }

    private val BYTE_MASK: BigInteger = BigInteger.valueOf(0xff)

    private val ED25519_P: BigInteger = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19))

    private val ED25519_D: BigInteger = BigInteger.valueOf(-121665)
        .multiply(BigInteger.valueOf(121666).modInverse(ED25519_P))
        .mod(ED25519_P)

    private val ED25519_BASE_POINT: Point =
        Point(
            BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202"),
            BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960")
        )

    private val ED25519_SPKI_PREFIX: ByteArray = byteArrayOf(
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    )

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

    private fun sha256Hex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
