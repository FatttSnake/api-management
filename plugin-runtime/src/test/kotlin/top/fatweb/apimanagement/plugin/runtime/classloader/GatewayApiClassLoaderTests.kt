package top.fatweb.apimanagement.plugin.runtime.classloader

import top.fatweb.apimanagement.api.v1.ApiPlugin
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class GatewayApiClassLoaderTests {
    private val platform = GatewayApiClassLoader(javaClass.classLoader)

    @Test
    fun `exposes the gateway api, the jdk and kotlin`() {
        assertSame(ApiPlugin::class.java, platform.loadClass(ApiPlugin::class.java.name))
        assertNotNull(platform.loadClass("kotlin.Unit"))
        assertNotNull(platform.loadClass("javax.sql.DataSource"))
        assertNotNull(platform.loadClass("java.util.List"))
    }

    @Test
    fun `hides gateway internals and third-party libraries`() {
        assertFailsWith<ClassNotFoundException> {
            platform.loadClass("fixture.a.FixtureA")
        }
        assertFailsWith<ClassNotFoundException> {
            platform.loadClass("tools.jackson.databind.json.JsonMapper")
        }
        assertFailsWith<ClassNotFoundException> {
            platform.loadClass("top.fatweb.apimanagement.plugin.runtime.classloader.ModuleKey")
        }
    }
}
