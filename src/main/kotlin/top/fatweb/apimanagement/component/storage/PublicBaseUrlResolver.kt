package top.fatweb.apimanagement.component.storage

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import top.fatweb.apimanagement.properties.ServerProperties

/**
 * Public base URL resolver
 *
 * Resolves the absolute base a login-free download URL is built from. The
 * configured value wins, because a deployment behind a reverse proxy cannot learn
 * its own public address from the request; otherwise the address of the current
 * request is used, which is correct for a direct deployment.
 *
 * Forwarded headers are deliberately not consulted: trusting them implicitly is
 * how a caller-controlled `Host` or `X-Forwarded-Proto` ends up interpolated into
 * a URL the gateway hands out. Configure the base URL explicitly in production
 * instead.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 */
@Component
class PublicBaseUrlResolver(
    private val serverProperties: ServerProperties
) {
    /**
     * Resolve the public base URL
     *
     * @return Base URL without a trailing slash, or null when it cannot be resolved
     *         because the call is outside a request and no base URL is configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(): String? {
        serverProperties.storage.publicBaseUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it.trimEnd('/') }

        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: return null

        val secure = request.isSecure
        val port = request.serverPort

        return buildString {
            append(if (secure) "https" else "http")
            append("://")
            append(request.serverName)

            val defaultPort = if (secure) 443 else 80
            if (port != defaultPort) {
                append(':').append(port)
            }
        }
    }
}
