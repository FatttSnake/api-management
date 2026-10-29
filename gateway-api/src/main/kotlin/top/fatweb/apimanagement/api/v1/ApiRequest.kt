package top.fatweb.apimanagement.api.v1

import java.util.Collections
import java.util.TreeMap

/**
 * Immutable request snapshot handed to an [ApiHandler].
 *
 * The body is raw bytes. A plugin that needs structured JSON uses [JsonCodec] from the gateway
 * context; the gateway does not deserialize into plugin classes.
 *
 * @property method HTTP method
 * @property path full request path
 * @property apiVersion API version taken from the URL
 * @property pathParameters path parameters, case-sensitive
 * @property queryParameters query parameters, case-insensitive names
 * @property headers request headers, case-insensitive names
 * @property principal caller identity, or null when anonymous
 * @property traceId gateway trace ID
 * @property interfaceInfo runtime snapshot of the matched interface
 * @property body raw request body
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ApiRequest(
    val method: String,
    val path: String,
    val apiVersion: Int,
    pathParameters: Map<String, String> = emptyMap(),
    queryParameters: Map<String, List<String>> = emptyMap(),
    headers: Map<String, List<String>> = emptyMap(),
    val principal: ApiPrincipal? = null,
    val traceId: String = "",
    val interfaceInfo: InterfaceInfo,
    val body: ByteArray = ByteArray(0),
) {
    /**
     * Path parameters.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pathParameters: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(pathParameters))

    /**
     * Query parameters.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val queryParameters: Map<String, List<String>> = normalizeMultimap(queryParameters)

    /**
     * Request headers.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val headers: Map<String, List<String>> = normalizeMultimap(headers)

    /**
     * Returns a path parameter.
     *
     * @param name parameter name
     * @return value or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun pathParam(name: String): String? = pathParameters[name]

    /**
     * Returns the first query parameter value.
     *
     * @param name parameter name
     * @return value or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun queryParam(name: String): String? = queryParameters[name]?.firstOrNull()

    /**
     * Returns all values of a query parameter.
     *
     * @param name parameter name
     * @return values
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun queryParams(name: String): List<String> = queryParameters[name].orEmpty()

    /**
     * Returns the first header value.
     *
     * @param name header name
     * @return value or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun header(name: String): String? = headers[name]?.firstOrNull()

    /**
     * Decodes the body as UTF-8 text.
     *
     * @return body text
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun bodyText(): String = body.toString(Charsets.UTF_8)

    private companion object {
        fun normalizeMultimap(source: Map<String, List<String>>): Map<String, List<String>> {
            if (source.isEmpty()) {
                return emptyMap()
            }
            val copy = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
            source.forEach { (name, values) -> copy[name] = values.toList() }
            return Collections.unmodifiableMap(copy)
        }
    }
}
