package top.fatweb.apimanagement.properties

import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Security properties tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 * @see SecurityProperties
 */
class SecurityPropertiesTests {
    @Test
    fun `client ip header requires trusted proxies`() {
        assertFailsWith<IllegalArgumentException> {
            SecurityProperties(clientIpHeader = "CF-Connecting-IP")
        }
    }
}
