package top.fatweb.apimanagement.component.plugin

import com.sun.net.httpserver.HttpServer
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.properties.PluginProperties
import top.fatweb.apimanagement.properties.ServerProperties
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Plugin module downloader tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginModuleDownloader
 */
class PluginModuleDownloaderTests {
    private var server: HttpServer? = null

    @AfterTest
    fun stopServer() {
        server?.stop(0)
    }

    @Test
    fun `downloads a module and verifies its digest`() {
        val bytes = "module bytes".toByteArray()
        val port = serve(bytes)
        var progress = 0L

        val downloaded =
            downloader(port).download(module(bytes)) { progress = it }

        assertContentEquals(bytes, downloaded.bytes)
        assertEquals("http://127.0.0.1:$port/", downloaded.source)
        assertEquals(bytes.size.toLong(), progress)
    }

    @Test
    fun `rejects a module whose digest differs from the lock`() {
        val expected = "expected".toByteArray()
        val port = serve("served".toByteArray())

        assertFailsWith<PluginInstallException> {
            downloader(port).download(module(expected))
        }
    }

    @Test
    fun `rejects an invalid coordinate segment`() {
        val bytes = "module bytes".toByteArray()
        val port = serve(bytes)
        val module = LockedModule("com.example", "..", "1.0.0", sha256(bytes), bytes.size.toLong())

        assertFailsWith<PluginInstallException> {
            downloader(port).download(module)
        }
    }

    @Test
    fun `rejects a download when online mode is off`() {
        val bytes = "module bytes".toByteArray()

        val error = assertFailsWith<PluginInstallException> {
            downloader(0, online = false).download(module(bytes))
        }

        assertTrue(error.message.orEmpty().contains("disabled"))
    }

    private fun serve(bytes: ByteArray): Int {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/") { exchange ->
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { body -> body.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        return httpServer.address.port
    }

    private fun downloader(port: Int, online: Boolean = true) = PluginModuleDownloader(
        PluginProperties(
            PluginProperties.ModuleProperties(
                online = online,
                repositories = listOf("http://127.0.0.1:$port/"),
            ),
        ),
        ServerProperties(),
    )

    private fun module(bytes: ByteArray) = LockedModule(
        group = "com.example",
        name = "demo",
        version = "1.0.0",
        sha256 = sha256(bytes),
        size = bytes.size.toLong(),
    )

    private fun sha256(bytes: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
