package top.fatweb.apimanagement.sdk.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin SDK version tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginSdk
 */
class PluginSdkTests {
    @Test
    fun `the version is baked into the jar`() {
        assertTrue(PluginSdk.version.isNotBlank())
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(PluginSdk.isNewer(PluginSdk.version))
        assertFalse(PluginSdk.isNewer("1.2.0", "1.2.0"))
        assertFalse(PluginSdk.isNewer("1.2", "1.2.0"))
    }

    @Test
    fun `a higher numeric core is newer`() {
        assertTrue(PluginSdk.isNewer("1.2.1", "1.2.0"))
        assertTrue(PluginSdk.isNewer("2.0", "1.9.9"))
        assertFalse(PluginSdk.isNewer("1.9.9", "2.0"))
    }

    @Test
    fun `a release is newer than its pre-release`() {
        assertTrue(PluginSdk.isNewer("1.0.0", "1.0.0-SNAPSHOT"))
        assertFalse(PluginSdk.isNewer("1.0.0-SNAPSHOT", "1.0.0"))
    }
}
