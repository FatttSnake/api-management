package top.fatweb.apimanagement.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import java.util.Base64

/**
 * Plugin crypto util tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginCryptoUtil
 */
class PluginCryptoUtilTests {
    private val secret = "test-token-secret"
    private val plaintext = "p@ssw0rd-with-symbols-§"

    @Test
    fun `a round trip returns the plaintext`() {
        assertEquals(plaintext, PluginCryptoUtil.decrypt(secret, PluginCryptoUtil.encrypt(secret, plaintext)))
    }

    @Test
    fun `the same plaintext never encrypts twice the same way`() {
        // A fresh IV per call is what keeps a stored credential from being recognisable
        // by comparing ciphertexts
        assertNotEquals(PluginCryptoUtil.encrypt(secret, plaintext), PluginCryptoUtil.encrypt(secret, plaintext))
    }

    @Test
    fun `another secret fails to decrypt`() {
        val ciphertext = PluginCryptoUtil.encrypt(secret, plaintext)

        assertFailsWith<Exception> { PluginCryptoUtil.decrypt("other-secret", ciphertext) }
    }

    @Test
    fun `a tampered ciphertext fails to decrypt`() {
        val encrypted = PluginCryptoUtil.encrypt(secret, plaintext)
        val decoded = Base64.getDecoder().decode(encrypted)
        // Flip the last byte, which is inside the GCM tag
        decoded[decoded.size - 1] = (decoded[decoded.size - 1] + 1).toByte()
        val tampered = Base64.getEncoder().encodeToString(decoded)

        assertFailsWith<Exception> { PluginCryptoUtil.decrypt(secret, tampered) }
    }

    @Test
    fun `a truncated ciphertext fails to decrypt`() {
        val encrypted = PluginCryptoUtil.encrypt(secret, plaintext)
        val truncated = Base64.getEncoder().encodeToString(Base64.getDecoder().decode(encrypted).copyOf(6))

        assertFailsWith<Exception> { PluginCryptoUtil.decrypt(secret, truncated) }
    }
}
