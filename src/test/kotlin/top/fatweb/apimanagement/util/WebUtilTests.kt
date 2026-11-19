package top.fatweb.apimanagement.util

import org.springframework.mock.web.MockHttpServletRequest
import top.fatweb.apimanagement.properties.SecurityProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Web util tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 * @see getRequestIp
 */
class WebUtilTests {
    @Test
    fun `remote address is used when proxy headers are not trusted`() {
        val request = request()
        request.addHeader("X-Real-IP", CLIENT_IP)
        request.addHeader("X-Forwarded-For", CLIENT_IP)

        assertEquals(EDGE_IP, getRequestIp(request, SecurityProperties()))
    }

    @Test
    fun `configured client ip header wins over the forwarded chain`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Real-IP", "198.51.100.8")
        request.addHeader("X-Forwarded-For", "198.51.100.8, 198.51.100.9")
        request.addHeader(CLIENT_IP_HEADER, CLIENT_IP)

        assertEquals(CLIENT_IP, getRequestIp(request, security(clientIpHeader = CLIENT_IP_HEADER)))
    }

    @Test
    fun `client ip header is ignored when not configured`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Forwarded-For", CLIENT_IP)
        request.addHeader(CLIENT_IP_HEADER, FORGED_IP)

        assertEquals(CLIENT_IP, getRequestIp(request, security()))
    }

    @Test
    fun `client ip header is ignored from an untrusted peer`() {
        val request = request()
        request.addHeader("X-Forwarded-For", CLIENT_IP)
        request.addHeader(CLIENT_IP_HEADER, FORGED_IP)

        assertEquals(EDGE_IP, getRequestIp(request, security(clientIpHeader = CLIENT_IP_HEADER)))
    }

    @Test
    fun `real ip is used when no forwarded chain is present`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Real-IP", CLIENT_IP)

        assertEquals(CLIENT_IP, getRequestIp(request, security()))
    }

    @Test
    fun `first forwarded address is used without trusted proxies`() {
        val request = request()
        request.addHeader("X-Forwarded-For", "$CLIENT_IP, 198.51.100.8")

        assertEquals(CLIENT_IP, getRequestIp(request, SecurityProperties(trustProxyHeaders = true)))
    }

    @Test
    fun `remote address is used when proxy headers are blank or unknown`() {
        val request = request()
        request.addHeader("X-Real-IP", " ")
        request.addHeader("X-Forwarded-For", "unknown")

        assertEquals(EDGE_IP, getRequestIp(request, security()))
    }

    @Test
    fun `headers are ignored when the peer is not a trusted proxy`() {
        val request = request()
        request.addHeader("X-Forwarded-For", CLIENT_IP)

        assertEquals(EDGE_IP, getRequestIp(request, security()))
    }

    @Test
    fun `forwarded chain is walked from the right past trusted hops`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Forwarded-For", "$CLIENT_IP, 203.0.113.11")

        assertEquals(CLIENT_IP, getRequestIp(request, security()))
    }

    @Test
    fun `client prepended forwarded entries do not win`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Forwarded-For", "203.0.113.66, $CLIENT_IP")

        assertEquals(CLIENT_IP, getRequestIp(request, security()))
    }

    @Test
    fun `non-address forwarded entries are skipped`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Forwarded-For", "$CLIENT_IP, not-an-ip")

        assertEquals(CLIENT_IP, getRequestIp(request, security()))
    }

    @Test
    fun `remote address is used when every forwarded hop is trusted`() {
        val request = request(remoteAddr = TRUSTED_EDGE)
        request.addHeader("X-Forwarded-For", "203.0.113.11")

        assertEquals(TRUSTED_EDGE, getRequestIp(request, security()))
    }

    @Test
    fun `ip matches addresses and cidr ranges`() {
        assertTrue(ipMatches("203.0.113.9", "203.0.113.9"))
        assertTrue(ipMatches("203.0.113.0/24", "203.0.113.9"))
        assertTrue(ipMatches("203.0.113.0/31", "203.0.113.1"))
        assertFalse(ipMatches("203.0.113.0/24", "203.0.114.9"))
        assertTrue(ipMatches("2001:db8::/32", "2001:db8::1"))
        assertFalse(ipMatches("2001:db8::/32", "2001:db9::1"))
        assertTrue(ipMatches("2001:db8::1", "2001:db8:0:0:0:0:0:1"))
        assertTrue(ipMatches("::1", "0:0:0:0:0:0:0:1"))
        assertFalse(ipMatches("2001:db8::1", "2001:db8::2"))
        assertTrue(ipMatches("::ffff:192.0.2.1", "192.0.2.1"))
        assertFalse(ipMatches("203.0.113.0/24", "example.com"))
    }

    private fun request(remoteAddr: String = EDGE_IP) = MockHttpServletRequest().apply { this.remoteAddr = remoteAddr }

    private fun security(clientIpHeader: String? = null) = SecurityProperties(
        trustProxyHeaders = true,
        trustedProxies = listOf(TRUSTED_RANGE),
        clientIpHeader = clientIpHeader,
    )

    companion object {
        private const val CLIENT_IP = "198.51.100.7"
        private const val FORGED_IP = "198.51.100.200"
        private const val EDGE_IP = "172.68.1.1"
        private const val TRUSTED_RANGE = "203.0.113.0/24"
        private const val TRUSTED_EDGE = "203.0.113.10"
        private const val CLIENT_IP_HEADER = "CF-Connecting-IP"
    }
}
