package top.fatweb.apimanagement.plugin.signing

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin signer tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginSigner
 */
class PluginSignerTests {
    private val keyPair = PluginSigner.generateKeyPair()
    private val publicPem = PluginSigner.publicKeyPem(keyPair.public)
    private val privatePem = PluginSigner.privateKeyPem(keyPair.private)

    @Test
    fun `a signed jar verifies`() {
        val jar = unsignedJar()

        PluginSigner.signAndEmbed(jar, privatePem)

        assertTrue(PluginSigner.verify(jar))
        assertTrue(PluginSigner.verify(jar, publicPem))
    }

    @Test
    fun `a tampered jar does not verify`() {
        val jar = unsignedJar()
        PluginSigner.signAndEmbed(jar, privatePem)

        rewrite(jar, "hello.txt", "tampered")

        assertFalse(PluginSigner.verify(jar))
    }

    @Test
    fun `another key does not verify the signature`() {
        val jar = unsignedJar()
        PluginSigner.signAndEmbed(jar, privatePem)
        val other = PluginSigner.publicKeyPem(PluginSigner.generateKeyPair().public)

        assertFalse(PluginSigner.verify(jar, other))
    }

    @Test
    fun `the fingerprint is stable and hexadecimal`() {
        val fingerprint = PluginSigner.spkiFingerprint(publicPem)

        assertTrue(Regex("^[0-9a-f]{64}$").matches(fingerprint))
        assertTrue(fingerprint == PluginSigner.spkiFingerprint(publicPem))
    }

    private fun unsignedJar(): Path {
        val jar = Files.createTempFile("plugin-", ".jar")
        write(jar) {
            putNextEntry(JarEntry(PluginSigner.PUBLIC_KEY_ENTRY))
            write(publicPem.toByteArray())
            closeEntry()
            putNextEntry(JarEntry("hello.txt"))
            write("hello".toByteArray())
            closeEntry()
        }
        return jar
    }

    private fun rewrite(jar: Path, name: String, content: String) {
        val contents = JarFileContents.read(jar)
        write(jar) {
            contents.forEach { (entryName, bytes) ->
                putNextEntry(JarEntry(entryName))
                write(if (entryName == name) content.toByteArray() else bytes)
                closeEntry()
            }
        }
    }

    private fun write(jar: Path, block: JarOutputStream.() -> Unit) {
        JarOutputStream(Files.newOutputStream(jar)).use(block)
    }

    private object JarFileContents {
        fun read(jar: Path): Map<String, ByteArray> = java.util.jar.JarFile(jar.toFile()).use { file ->
            file.entries().asSequence()
                .filterNot { it.isDirectory }
                .associate { entry ->
                    entry.name to file.getInputStream(entry).use { it.readBytes() }
                }
        }
    }
}
