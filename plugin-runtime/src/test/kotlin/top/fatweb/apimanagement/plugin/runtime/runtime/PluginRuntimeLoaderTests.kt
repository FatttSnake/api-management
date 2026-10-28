package top.fatweb.apimanagement.plugin.runtime.runtime

import fixture.plugin.FixtureApiPlugin
import fixture.plugin.PingHandler
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import top.fatweb.apimanagement.plugin.runtime.classloader.GatewayApiClassLoader
import top.fatweb.apimanagement.plugin.runtime.classloader.PluginModuleRegistry
import top.fatweb.apimanagement.plugin.runtime.jarOf
import top.fatweb.apimanagement.plugin.runtime.module.FileSystemPluginModuleStore
import top.fatweb.apimanagement.plugin.runtime.route.PluginApiHandlerAdapter
import top.fatweb.apimanagement.plugin.runtime.route.PluginApiHandlerMapping
import top.fatweb.apimanagement.plugin.runtime.route.PluginRouteRegistry
import top.fatweb.apimanagement.plugin.runtime.testServices
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginRuntimeLoaderTests {
    @TempDir
    lateinit var root: Path

    @Test
    fun `loads a plugin and serves a route end to end`() {
        val platform = GatewayApiClassLoader(javaClass.classLoader)
        val store = FileSystemPluginModuleStore(root.resolve("modules"))
        val moduleRegistry = PluginModuleRegistry(store, platform)
        val loader = PluginRuntimeLoader(platform, moduleRegistry)
        val pluginJar = root.resolve("fixture.jar")
        Files.write(
            pluginJar,
            jarOf(
                FixtureApiPlugin::class,
                PingHandler::class,
                entries = mapOf(
                    PluginRuntimeLoader.DESCRIPTOR_ENTRY to descriptorJson().toByteArray(),
                    PluginRuntimeLoader.LOCK_ENTRY to """{"format":1,"modules":[]}""".toByteArray()
                )
            )
        )

        val loaded = loader.load(pluginJar, testServices())
        assertEquals("fixture", loaded.descriptor.pluginId)
        assertEquals(listOf("api:fixture:v1:ping"), loaded.routes.map { it.apiCode })

        val routes = PluginRouteRegistry()
        routes.replace("fixture", loaded.routes)
        val mapping = PluginApiHandlerMapping(routes)
        val request = MockHttpServletRequest("GET", "/api/fixture/v1/ping/world")
        val chain = assertNotNull(mapping.getHandler(request))
        val response = MockHttpServletResponse()

        PluginApiHandlerAdapter(jsonMapper()).handle(request, response, chain.handler)

        assertEquals(200, response.status)
        assertTrue(response.getContentAsString().contains("\"message\":\"hello, world\""))
        assertTrue(response.getContentAsString().contains("\"plugin\":\"fixture\""))

        loaded.stop()
        loaded.uninstall()
        loaded.close()
        moduleRegistry.close()
    }

    @Test
    fun `serves a higher requested version from a lower declared route`() {
        val platform = GatewayApiClassLoader(javaClass.classLoader)
        val store = FileSystemPluginModuleStore(root.resolve("modules-v2"))
        val moduleRegistry = PluginModuleRegistry(store, platform)
        val loader = PluginRuntimeLoader(platform, moduleRegistry)
        val pluginJar = root.resolve("fixture-v2.jar")
        Files.write(
            pluginJar,
            jarOf(
                FixtureApiPlugin::class,
                PingHandler::class,
                entries = mapOf(
                    PluginRuntimeLoader.DESCRIPTOR_ENTRY to descriptorJson().toByteArray(),
                    PluginRuntimeLoader.LOCK_ENTRY to """{"format":1,"modules":[]}""".toByteArray()
                )
            )
        )
        val loaded = loader.load(pluginJar, testServices())
        val routes = PluginRouteRegistry()
        routes.replace("fixture", loaded.routes)

        val match = assertNotNull(routes.match("GET", "/api/fixture/v3/ping/world"))
        assertEquals(3, match.requestedVersion)
        assertEquals("world", match.pathParameters["name"])

        loaded.close()
        moduleRegistry.close()
    }

    @Test
    fun `refuses a plugin with declared dependencies but no lock`() {
        val platform = GatewayApiClassLoader(javaClass.classLoader)
        val store = FileSystemPluginModuleStore(root.resolve("modules-deps"))
        val moduleRegistry = PluginModuleRegistry(store, platform)
        val loader = PluginRuntimeLoader(platform, moduleRegistry)
        val pluginJar = root.resolve("fixture-deps.jar")
        Files.write(
            pluginJar,
            jarOf(
                FixtureApiPlugin::class,
                PingHandler::class,
                entries = mapOf(
                    PluginRuntimeLoader.DESCRIPTOR_ENTRY to
                        descriptorJson("""{"fixture:a":"1.0.0"}""").toByteArray(),
                    PluginRuntimeLoader.LOCK_ENTRY to """{"format":1,"modules":[]}""".toByteArray()
                )
            )
        )

        assertFailsWith<PluginLoadException> {
            loader.load(pluginJar, testServices())
        }
        moduleRegistry.close()
    }

    @Test
    fun `rejects an unsupported descriptor format`() {
        assertFailsWith<PluginDescriptorException> {
            PluginDescriptorParser().parse(descriptorJson().replace("\"format\": 1", "\"format\": 2"))
        }
    }

    private fun descriptorJson(dependencies: String = "{}"): String =
        """
        {
          "format": 1,
          "pluginId": "fixture",
          "name": "Fixture",
          "versionName": "1.0.0",
          "versionCode": 1,
          "mainClass": "fixture.plugin.FixtureApiPlugin",
          "requiresGateway": ">=1.0.0 <2.0.0",
          "requiresGatewayApi": "^1.0",
          "dependencies": $dependencies
        }
        """.trimIndent()

    private fun jsonMapper(): JsonMapper =
        JsonMapper.builder().addModules(KotlinModule.Builder().build()).build()
}
