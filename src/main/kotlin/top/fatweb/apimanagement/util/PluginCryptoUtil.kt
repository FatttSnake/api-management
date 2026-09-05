package top.fatweb.apimanagement.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Plugin crypto util
 *
 * AES-256-GCM encryption for plugin datasource credentials before they are stored.
 * The key is derived by SHA-256 of the gateway's token secret; the ciphertext is
 * `base64(iv || ciphertext)`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginCryptoUtil {
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_LENGTH = 12

    /**
     * Encrypt plaintext with an AES-256-GCM key derived from the secret
     *
     * @param secret Key derivation secret
     * @param plaintext Plaintext
     * @return base64(iv + ciphertext)
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun encrypt(secret: String, plaintext: String): String {
        val key = deriveKey(secret)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    /**
     * Decrypt ciphertext produced by [encrypt]
     *
     * @param secret Key derivation secret
     * @param ciphertext base64(iv + ciphertext)
     * @return Plaintext
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun decrypt(secret: String, ciphertext: String): String {
        val key = deriveKey(secret)
        val decoded = Base64.getDecoder().decode(ciphertext)
        val iv = decoded.copyOfRange(0, GCM_IV_LENGTH)
        val body = decoded.copyOfRange(GCM_IV_LENGTH, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    private fun deriveKey(secret: String): SecretKeySpec =
        SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8)), "AES")
}
