package top.fatweb.apimanagement.plugin.runtime.route

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerAdapter
import org.springframework.web.servlet.ModelAndView
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.ApiException
import top.fatweb.apimanagement.api.v1.ApiPrincipal
import top.fatweb.apimanagement.api.v1.ApiRequest
import top.fatweb.apimanagement.api.v1.ApiResponse
import top.fatweb.apimanagement.api.v1.InterfaceInfo
import java.util.Collections

/**
 * Invokes a plugin handler selected by [PluginApiHandlerMapping].
 *
 * The adapter builds a neutral [ApiRequest], switches the thread context class loader to the
 * plugin's, maps [ApiException] to the business envelope and writes either the envelope or a raw
 * response.
 *
 * @param jsonMapper gateway JSON mapper
 * @param principalResolver resolves the caller identity
 * @param interfaceResolver resolves the interface snapshot
 * @param traceIdResolver resolves the trace ID
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginApiHandlerAdapter(
    private val jsonMapper: JsonMapper,
    private val principalResolver: PluginPrincipalResolver = PluginPrincipalResolver { null },
    private val interfaceResolver: PluginInterfaceResolver = PluginInterfaceResolver { invocation ->
        InterfaceInfo(
            code = invocation.apiCode,
            pluginId = invocation.pluginId,
            apiVersion = invocation.definition.apiVersion,
            name = invocation.definition.operation.summary,
            path = invocation.definition.path,
            method = invocation.definition.method
        )
    },
    private val traceIdResolver: PluginTraceIdResolver = PluginTraceIdResolver { request ->
        request.getHeader("X-Trace-Id") ?: request.getHeader("X-Request-Id") ?: ""
    }
) : HandlerAdapter {
    private val logger = LoggerFactory.getLogger(PluginApiHandlerAdapter::class.java)

    /**
     * Checks whether the handler is a plugin invocation.
     *
     * @param handler handler
     * @return true for [PluginInvocation]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun supports(handler: Any): Boolean = handler is PluginInvocation

    /**
     * Invokes the plugin.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param handler plugin invocation
     * @return always null, because the response is written directly
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): ModelAndView? {
        val invocation = handler as PluginInvocation
        val apiRequest = toApiRequest(request, invocation)
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = invocation.classLoader
        val apiResponse = try {
            invocation.route.handler.handle(apiRequest)
        } catch (ex: ApiException) {
            ApiResponse.fail(ex.code, ex.message ?: "", ex.data).withHttpStatus(ex.httpStatus)
        } catch (ex: Exception) {
            logger.error(
                "Plugin {} failed to handle {} {}",
                invocation.pluginId,
                request.method,
                request.requestURI,
                ex
            )
            ApiResponse.fail(500, "Internal error").withHttpStatus(500)
        } catch (ex: LinkageError) {
            logger.error(
                "Plugin {} has a linkage failure handling {} {}",
                invocation.pluginId,
                request.method,
                request.requestURI,
                ex
            )
            ApiResponse.fail(500, "Internal error").withHttpStatus(500)
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
        write(response, apiResponse)
        return null
    }

    private fun toApiRequest(
        request: HttpServletRequest,
        invocation: PluginInvocation
    ): ApiRequest {
        val headers = Collections.list(request.headerNames)
            .associateWith { name -> request.getHeaders(name).toList() }
        return ApiRequest(
            method = request.method,
            path = request.requestURI,
            apiVersion = invocation.match.requestedVersion,
            pathParameters = invocation.match.pathParameters,
            queryParameters = request.parameterMap.mapValues { (_, values) -> values.toList() },
            headers = headers,
            principal = principalResolver.resolve(request),
            traceId = traceIdResolver.resolve(request),
            interfaceInfo = interfaceResolver.resolve(invocation),
            body = request.inputStream.readBytes()
        )
    }

    private fun write(response: HttpServletResponse, apiResponse: ApiResponse) {
        apiResponse.headers.forEach { (name, value) -> response.setHeader(name, value) }
        response.status = apiResponse.httpStatus
        if (apiResponse.isRaw) {
            response.contentType = apiResponse.contentType ?: "application/octet-stream"
            response.outputStream.write(apiResponse.rawBody ?: ByteArray(0))
        } else {
            response.contentType = "application/json;charset=UTF-8"
            val bytes = try {
                jsonMapper.writeValueAsBytes(
                    Envelope(
                        code = apiResponse.code,
                        success = apiResponse.success,
                        msg = apiResponse.message,
                        data = apiResponse.data
                    )
                )
            } catch (ex: Exception) {
                logger.error("Cannot serialize plugin response", ex)
                response.status = 500
                INTERNAL_ERROR
            }
            response.outputStream.write(bytes)
        }
        response.outputStream.flush()
    }

    private data class Envelope(
        val code: Int,
        val success: Boolean,
        val msg: String,
        val data: Any?
    )

    private companion object {
        val INTERNAL_ERROR: ByteArray =
            """{"code":500,"success":false,"msg":"Internal error"}""".toByteArray()
    }
}

/**
 * Resolves the caller identity.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun interface PluginPrincipalResolver {
    /**
     * Resolves a principal.
     *
     * @param request HTTP request
     * @return principal or null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(request: HttpServletRequest): ApiPrincipal?
}

/**
 * Resolves the interface snapshot for a route.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun interface PluginInterfaceResolver {
    /**
     * Resolves interface info.
     *
     * @param invocation plugin invocation
     * @return interface snapshot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(invocation: PluginInvocation): InterfaceInfo
}

/**
 * Resolves the trace ID.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun interface PluginTraceIdResolver {
    /**
     * Resolves a trace ID.
     *
     * @param request HTTP request
     * @return trace ID, never null
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun resolve(request: HttpServletRequest): String
}
