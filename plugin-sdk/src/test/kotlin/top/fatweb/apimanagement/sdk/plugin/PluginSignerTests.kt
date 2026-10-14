package top.fatweb.apimanagement.sdk.plugin

import java.nio.file.Files
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Plugin signer tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginSigner
 */
class PluginSignerTests {
    @Test
    fun `the derived public key equals the generated one`() {
        val pair = PluginSigner.generateKeyPair()

        assertEquals(
            PluginSigner.publicKeyPem(pair.public),
            PluginSigner.publicKeyPemFromPrivate(PluginSigner.privateKeyPem(pair.private))
        )
    }

    @Test
    fun `the derived public key is the same key the gateway would trust`() {
        val pair = PluginSigner.generateKeyPair()
        val derived = PluginSigner.publicKeyPemFromPrivate(PluginSigner.privateKeyPem(pair.private))

        assertEquals(
            PluginSigner.spkiFingerprint(PluginSigner.publicKeyPem(pair.public)),
            PluginSigner.spkiFingerprint(derived)
        )
    }

    @Test
    fun `a jar signed with the private key verifies against the derived public key`() {
        val pair = PluginSigner.generateKeyPair()
        val derived = PluginSigner.publicKeyPemFromPrivate(PluginSigner.privateKeyPem(pair.private))
        val jar = Files.createTempFile("plugin", ".jar")
        try {
            JarOutputStream(Files.newOutputStream(jar)).use { }
            PluginSigner.signAndEmbed(jar, PluginSigner.privateKeyPem(pair.private))

            assertTrue(PluginSigner.verify(jar, derived))
        } finally {
            Files.deleteIfExists(jar)
        }
    }

    @Test
    fun `a malformed private key is rejected`() {
        assertFailsWith<Exception> { PluginSigner.publicKeyPemFromPrivate("not a pem") }
    }
}
