package top.fatweb.apimanagement.plugin.runtime.route

import top.fatweb.apimanagement.api.v1.ApiResponse
import top.fatweb.apimanagement.api.v1.OperationMetadata
import top.fatweb.apimanagement.api.v1.RouteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Route registry tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginRouteRegistry
 */
class PluginRouteRegistryTests {
    private val registry = PluginRouteRegistry()

    @Test
    fun `a lower declared version serves a higher requested version`() {
        registry.replace("echo", listOf(route("echo", 1, "/ping")))

        val match = registry.match("GET", "/api/echo/v2/ping")

        assertEquals(1, match?.route?.definition?.apiVersion)
        assertEquals(2, match?.requestedVersion)
    }

    @Test
    fun `the highest declared version wins`() {
        registry.replace(
            "echo",
            listOf(
                route("echo", 1, "/ping", operationId = "pingV1"),
                route("echo", 2, "/ping", operationId = "pingV2"),
            ),
        )

        assertEquals("pingV2", registry.match("GET", "/api/echo/v2/ping")?.route?.apiCode?.substringAfterLast(':'))
        assertEquals("pingV1", registry.match("GET", "/api/echo/v1/ping")?.route?.apiCode?.substringAfterLast(':'))
    }

    @Test
    fun `a path parameter is extracted`() {
        registry.replace("filebox", listOf(route("filebox", 1, "/files/{id}")))

        val match = registry.match("GET", "/api/filebox/v1/files/42")

        assertEquals("42", match?.pathParameters?.get("id"))
    }

    @Test
    fun `replacing a plugin leaves other plugins alone`() {
        registry.replace("echo", listOf(route("echo", 1, "/ping")))
        registry.replace("filebox", listOf(route("filebox", 1, "/files")))

        registry.replace("echo", listOf(route("echo", 1, "/pong")))

        assertNull(registry.match("GET", "/api/echo/v1/ping"))
        assertEquals("echo", registry.match("GET", "/api/echo/v1/pong")?.route?.pluginId)
        assertEquals("filebox", registry.match("GET", "/api/filebox/v1/files")?.route?.pluginId)
    }

    @Test
    fun `two routes with one operation id are refused`() {
        assertFailsWith<IllegalArgumentException> {
            registry.replace(
                "echo",
                listOf(
                    route("echo", 1, "/ping"),
                    route("echo", 1, "/pong"),
                ),
            )
        }
    }

    @Test
    fun `a route of another plugin is refused`() {
        assertFailsWith<IllegalArgumentException> {
            registry.replace("echo", listOf(route("filebox", 1, "/ping")))
        }
    }

    private fun route(pluginId: String, version: Int, path: String, operationId: String = "ping"): RegisteredRoute =
        RegisteredRoute(
            pluginId = pluginId,
            definition = RouteDefinition(
                method = "GET",
                path = path,
                apiVersion = version,
                operation = OperationMetadata(operationId = operationId),
            ),
            handler = { ApiResponse.ok() },
            classLoader = javaClass.classLoader,
        )
}
