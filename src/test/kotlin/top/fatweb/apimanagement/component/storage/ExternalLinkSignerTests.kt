package top.fatweb.apimanagement.component.storage

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import top.fatweb.apimanagement.properties.SecurityProperties
import top.fatweb.apimanagement.properties.ServerProperties

/**
 * External link signer tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ExternalLinkSigner
 */
class ExternalLinkSignerTests {
    private val signer = ExternalLinkSigner(ServerProperties())
    private val key = "alpha/files/avatar/1.png"
    private val expiresAt = 1_800_000_000L

    @Test
    fun `a minted signature verifies`() {
        assertTrue(signer.verify(key, expiresAt, signer.sign(key, expiresAt)))
    }

    @Test
    fun `a signature does not transfer to another object`() {
        val signature = signer.sign(key, expiresAt)

        assertFalse(signer.verify("alpha/files/avatar/2.png", expiresAt, signature))
        assertFalse(signer.verify("beta/files/avatar/1.png", expiresAt, signature))
    }

    @Test
    fun `a signature does not transfer to another expiry`() {
        // Otherwise a holder could extend their own link simply by editing e.
        assertFalse(signer.verify(key, expiresAt + 1, signer.sign(key, expiresAt)))
    }

    @Test
    fun `a malformed signature is rejected rather than raised`() {
        listOf("", "!!!not base64!!!", "AAAA", "  ", "aaaa bbbb").forEach {
            assertFalse(signer.verify(key, expiresAt, it), "expected '$it' to be rejected")
        }
    }

    @Test
    fun `the signature is URL safe and unpadded`() {
        val signature = signer.sign(key, expiresAt)

        assertTrue(signature.isNotEmpty())
        assertFalse(signature.contains('='))
        assertFalse(signature.contains('+'))
        assertFalse(signature.contains('/'))
    }

    @Test
    fun `changing the secret invalidates every link`() {
        val other = ExternalLinkSigner(ServerProperties(security = SecurityProperties(tokenSecret = "other")))

        assertNotEquals(signer.sign(key, expiresAt), other.sign(key, expiresAt))
        assertFalse(other.verify(key, expiresAt, signer.sign(key, expiresAt)))
    }
}
