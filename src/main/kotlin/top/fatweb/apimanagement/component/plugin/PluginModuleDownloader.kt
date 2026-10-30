package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.exception.PluginInstallException
import top.fatweb.apimanagement.plugin.runtime.module.LockedModule
import top.fatweb.apimanagement.properties.PluginProperties
import top.fatweb.apimanagement.properties.ServerProperties
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Downloads dependency modules from the configured Maven repositories.
 *
 * Only repositories configured on the gateway are used; the source recorded in a plugin lock
 * is never followed. A downloaded jar must match the digest and the size recorded in the lock.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginModuleDownloader(private val pluginProperties: PluginProperties, serverProperties: ServerProperties) {
    private val maxModuleBytes = serverProperties.storage.pluginModuleMaxBytes
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(pluginProperties.module.connectTimeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    /**
     * Whether online module download is enabled
     *
     * @return true when enabled
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val online: Boolean
        get() = pluginProperties.module.online

    /**
     * Downloads one module, trying every configured repository in order.
     *
     * @param module module declared by a plugin lock
     * @param onProgress called with the number of bytes received so far
     * @return downloaded bytes and the repository served them
     * @throws PluginInstallException when no repository serves a jar matching the lock
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun download(module: LockedModule, onProgress: (Long) -> Unit = {}): DownloadedModule {
        if (!online) {
            throw PluginInstallException("Online module download is disabled")
        }
        val failures = mutableListOf<String>()
        pluginProperties.module.repositories.forEach { repository ->
            onProgress(0)
            val bytes =
                try {
                    fetch(repository, module, onProgress)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw PluginInstallException("Interrupted while downloading ${module.coordinate}", ex)
                } catch (ex: Exception) {
                    failures.add("${repository.trimEnd('/')}: ${ex.message}")
                    null
                } ?: return@forEach
            val digest = sha256(bytes)
            if (digest != module.sha256) {
                failures.add("${repository.trimEnd('/')}: digest $digest does not match ${module.sha256}")
                return@forEach
            }
            if (module.size > 0 && bytes.size.toLong() != module.size) {
                failures.add("${repository.trimEnd('/')}: size ${bytes.size} does not match ${module.size}")
                return@forEach
            }
            return DownloadedModule(bytes, repository)
        }
        throw PluginInstallException(
            "Cannot download module ${module.coordinate}: ${failures.joinToString("; ")}",
        )
    }

    private fun fetch(repository: String, module: LockedModule, onProgress: (Long) -> Unit): ByteArray {
        val request =
            HttpRequest
                .newBuilder(uri(repository, module))
                .timeout(pluginProperties.module.requestTimeout)
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val body = response.body()
        try {
            if (response.statusCode() !in 200..299) {
                throw PluginInstallException("HTTP ${response.statusCode()}")
            }
            val declared = response.headers().firstValueAsLong("Content-Length").orElse(-1)
            if (declared > maxModuleBytes) {
                throw PluginInstallException("Module is $declared bytes, limit is $maxModuleBytes")
            }

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = body.read(buffer)
                if (read < 0) {
                    break
                }
                total += read
                if (total > maxModuleBytes) {
                    throw PluginInstallException("Module is larger than $maxModuleBytes bytes")
                }
                output.write(buffer, 0, read)
                onProgress(total)
            }
            return output.toByteArray()
        } finally {
            body.close()
        }
    }

    private fun uri(repository: String, module: LockedModule): URI {
        val group = module.group.split('.').joinToString("/") { segment(it) }
        val name = segment(module.name)
        val version = segment(module.version)
        return URI.create("${repository.trimEnd('/')}/$group/$name/$version/$name-$version.jar")
    }

    private fun segment(value: String): String {
        if (!SEGMENT.matches(value) || value == "." || value == "..") {
            throw PluginInstallException("Invalid Maven coordinate segment: $value")
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * A downloaded module jar.
     *
     * @property bytes jar bytes
     * @property source repository served the jar
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    class DownloadedModule(val bytes: ByteArray, val source: String)

    private companion object {
        val SEGMENT = Regex("^[A-Za-z0-9._+-]+$")
    }
}
