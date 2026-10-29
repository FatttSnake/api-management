package top.fatweb.apimanagement.api.v1

import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Response returned by an [ApiHandler].
 *
 * By default the gateway serializes `{code, success, msg, data}` and uses HTTP 200.
 * [raw] bypasses that envelope for file downloads and other non-JSON responses.
 *
 * @property httpStatus HTTP status
 * @property headers response headers
 * @property code business code
 * @property success whether the business call succeeded
 * @property message business message
 * @property data response payload
 * @property rawBody raw bytes for a non-envelope response
 * @property contentType content type for a raw response
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ApiResponse private constructor(
    val httpStatus: Int,
    headers: Map<String, String>,
    val code: Int,
    val success: Boolean,
    val message: String,
    val data: Any?,
    val rawBody: ByteArray?,
    val contentType: String?,
) {
    /**
     * Response headers.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val headers: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(headers))

    /**
     * Whether this response bypasses the business envelope.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val isRaw: Boolean
        get() = rawBody != null

    /**
     * Returns a copy with an added header.
     *
     * @param name header name
     * @param value header value
     * @return new response
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun withHeader(name: String, value: String): ApiResponse {
        val copy = LinkedHashMap(headers)
        copy[name] = value
        return ApiResponse(httpStatus, copy, code, success, message, data, rawBody, contentType)
    }

    /**
     * Returns a copy with another HTTP status.
     *
     * @param status HTTP status
     * @return new response
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun withHttpStatus(status: Int): ApiResponse =
        ApiResponse(status, headers, code, success, message, data, rawBody, contentType)

    companion object {
        /**
         * Business code used by a successful call.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val CODE_SUCCESS: Int = 0

        /**
         * Builds a successful response.
         *
         * @param data response payload
         * @return response
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun ok(data: Any? = null): ApiResponse =
            ApiResponse(200, emptyMap(), CODE_SUCCESS, true, "success", data, null, null)

        /**
         * Builds a successful response with a custom message.
         *
         * @param data response payload
         * @param message response message
         * @return response
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun ok(data: Any?, message: String): ApiResponse =
            ApiResponse(200, emptyMap(), CODE_SUCCESS, true, message, data, null, null)

        /**
         * Builds a business failure response.
         *
         * @param code business code
         * @param message failure message
         * @param data optional payload
         * @return response
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun fail(code: Int, message: String, data: Any? = null): ApiResponse =
            ApiResponse(200, emptyMap(), code, false, message, data, null, null)

        /**
         * Builds a raw response that bypasses the business envelope.
         *
         * @param httpStatus HTTP status
         * @param contentType content type
         * @param body response bytes
         * @return response
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun raw(httpStatus: Int, contentType: String?, body: ByteArray): ApiResponse = ApiResponse(
            httpStatus,
            emptyMap(),
            0,
            true,
            "",
            null,
            body,
            contentType ?: "application/octet-stream",
        )

        /**
         * Builds a plain-text raw response.
         *
         * @param httpStatus HTTP status
         * @param text response text
         * @return response
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @JvmStatic
        fun text(httpStatus: Int, text: String): ApiResponse =
            raw(httpStatus, "text/plain;charset=UTF-8", text.toByteArray(StandardCharsets.UTF_8))
    }
}
