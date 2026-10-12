package top.fatweb.apimanagement.component.api

import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * API version condition tests
 *
 * Pins how a request is matched against a controller's declared plugin and version - including
 * that the servlet context path comes off before matching, since reading the raw request URI
 * would silently 404 every plugin route on a gateway not deployed at the root.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiVersionCondition
 * @see ApiPath
 */
class ApiVersionConditionTests {
    private fun request(uri: String, contextPath: String = ""): MockHttpServletRequest =
        MockHttpServletRequest("GET", uri).apply { this.contextPath = contextPath }

    @Test
    fun `a request for the declared plugin and version matches`() {
        assertNotNull(ApiVersionCondition("echo", 1).getMatchingCondition(request("/api/echo/v1/ping")))
    }

    @Test
    fun `a request for a later version is served by the older controller`() {
        assertNotNull(ApiVersionCondition("echo", 1).getMatchingCondition(request("/api/echo/v3/ping")))
    }

    @Test
    fun `a request for an earlier version is not`() {
        assertNull(ApiVersionCondition("echo", 2).getMatchingCondition(request("/api/echo/v1/ping")))
    }

    @Test
    fun `a request for another plugin is not`() {
        assertNull(ApiVersionCondition("echo", 1).getMatchingCondition(request("/api/other/v1/ping")))
    }

    @Test
    fun `matching still works behind a servlet context path`() {
        val condition = ApiVersionCondition("echo", 1)

        assertNotNull(
            condition.getMatchingCondition(request("/gateway/api/echo/v1/ping", contextPath = "/gateway"))
        )
    }

    @Test
    fun `a URI that is not an API URL never matches`() {
        assertNull(ApiVersionCondition("echo", 1).getMatchingCondition(request("/system/user/list")))
    }
}
