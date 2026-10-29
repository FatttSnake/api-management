package top.fatweb.apimanagement.api.v1

import java.io.IOException
import java.net.URI
import java.time.Duration

/**
 * Outbound HTTP client owned by the gateway.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface HttpCapability {
    /**
     * Executes an HTTP request.
     *
     * @param request request
     * @return response
     * @throws IOException when the request cannot be completed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Throws(IOException::class)
    fun execute(request: HttpRequest): HttpResponse

    /**
     * One outbound request.
     *
     * @property method HTTP method
     * @property uri target URI
     * @property headers request headers
     * @property body request body
     * @property timeout request timeout, or null for the gateway default
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class HttpRequest @JvmOverloads constructor(
        val uri: URI,
        val method: String = "GET",
        val headers: Map<String, List<String>> = emptyMap(),
        val body: ByteArray = ByteArray(0),
        val timeout: Duration? = null,
    ) {
        init {
            require(method.isNotBlank()) { "method must not be blank" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HttpRequest

            if (uri != other.uri) return false
            if (method != other.method) return false
            if (headers != other.headers) return false
            if (!body.contentEquals(other.body)) return false
            if (timeout != other.timeout) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uri.hashCode()
            result = 31 * result + method.hashCode()
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            result = 31 * result + timeout.hashCode()
            return result
        }

        companion object {
            /**
             * Builds a GET request.
             *
             * @param uri target URI
             * @return request
             * @author FatttSnake, fatttsnake@gmail.com
             * @since 1.0.0
             */
            @JvmStatic
            fun get(uri: URI): HttpRequest = HttpRequest(uri)

            /**
             * Builds a POST request.
             *
             * @param uri target URI
             * @param body request body
             * @return request
             * @author FatttSnake, fatttsnake@gmail.com
             * @since 1.0.0
             */
            @JvmStatic
            fun post(uri: URI, body: ByteArray): HttpRequest = HttpRequest(uri, "POST", body = body)
        }
    }

    /**
     * One outbound response.
     *
     * @property status HTTP status
     * @property headers response headers
     * @property body response body
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class HttpResponse @JvmOverloads constructor(
        val status: Int,
        val headers: Map<String, List<String>> = emptyMap(),
        val body: ByteArray = ByteArray(0),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HttpResponse

            if (status != other.status) return false
            if (headers != other.headers) return false
            if (!body.contentEquals(other.body)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = status
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            return result
        }
    }
}
