package top.fatweb.apimanagement.component.plugin.runtime

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.api.v1.HttpCapability
import java.net.http.HttpClient
import java.time.Duration
import java.net.http.HttpRequest as JdkHttpRequest

/**
 * Outbound HTTP client for plugins.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginHttpClient {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Executes an outbound request.
     *
     * @param request request
     * @return response
     * @throws java.io.IOException when the request cannot be completed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Throws(java.io.IOException::class)
    fun execute(request: HttpCapability.HttpRequest): HttpCapability.HttpResponse {
        val publisher = if (request.body.isEmpty()) {
            JdkHttpRequest.BodyPublishers.noBody()
        } else {
            JdkHttpRequest.BodyPublishers.ofByteArray(request.body)
        }
        val builder = JdkHttpRequest.newBuilder(request.uri)
            .method(request.method, publisher)
        request.headers.forEach { (name, values) ->
            values.forEach { value -> builder.header(name, value) }
        }
        request.timeout?.let(builder::timeout)
        val response = client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray())
        return HttpCapability.HttpResponse(
            status = response.statusCode(),
            headers = response.headers().map(),
            body = response.body()
        )
    }

    /**
     * Returns a plugin-scoped facade.
     *
     * @return HTTP capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun capability(): HttpCapability = object : HttpCapability {
        override fun execute(request: HttpCapability.HttpRequest): HttpCapability.HttpResponse =
            this@PluginHttpClient.execute(request)
    }
}
