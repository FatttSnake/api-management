package top.fatweb.apimanagement.component.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.mvc.condition.RequestCondition

/**
 * Api version condition
 *
 * Matches `/api/{plugin}/v{version}/...` URLs against a controller's declared
 * plugin and version: the plugin ID must match exactly, and the requested
 * version must be >= the declared version (rolling forward compatibility).
 *
 * The URL is parsed by [ApiPath], which is where the servlet context path comes off - reading
 * the raw `requestURI` here would tie matching to a gateway deployed at the root.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RequestCondition
 * @see ApiPath
 * @see ApiVersionCondition
 */
class ApiVersionCondition(
    private val plugin: String,
    private val apiVersion: Int
) : RequestCondition<ApiVersionCondition> {

    override fun combine(other: ApiVersionCondition): ApiVersionCondition =
        ApiVersionCondition(other.plugin, other.apiVersion)

    override fun getMatchingCondition(request: HttpServletRequest): ApiVersionCondition? {
        val scoped = ApiPath.parse(request) ?: return null
        return if (scoped.plugin == plugin && scoped.apiVersion >= apiVersion) this else null
    }

    override fun compareTo(other: ApiVersionCondition, request: HttpServletRequest): Int =
        other.apiVersion - apiVersion
}
