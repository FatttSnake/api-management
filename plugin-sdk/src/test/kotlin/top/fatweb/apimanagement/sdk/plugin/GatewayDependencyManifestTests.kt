package top.fatweb.apimanagement.sdk.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Gateway dependency manifest tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see GatewayDependencyManifest
 */
class GatewayDependencyManifestTests {
    @Test
    fun `a manifest survives a round trip`() {
        val json = """
            {"format":1,"sdkVersion":"1.0.0","dependencies":{"org.flywaydb:flyway-core":"13.3.0"}}
        """.trimIndent()

        val manifest = GatewayDependencyManifest.read(json)

        assertEquals("1.0.0", manifest.sdkVersion)
        assertEquals("13.3.0", manifest.versionOf("org.flywaydb:flyway-core"))
        assertNull(manifest.versionOf("org.example:missing"))
    }

    @Test
    fun `the SDK carries the generated manifest`() {
        val json = GatewayDependencyManifest::class.java
            .getResourceAsStream("/${GatewayDependencyManifest.RESOURCE}")
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("The SDK jar carries no ${GatewayDependencyManifest.RESOURCE}")

        val manifest = GatewayDependencyManifest.read(json)

        assertEquals(PluginSdk.version, manifest.sdkVersion)
        assertNotNull(manifest.versionOf("org.flywaydb:flyway-core"))
    }

    @Test
    fun `an unknown format is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            GatewayDependencyManifest.read("""{"format":2,"dependencies":{}}""")
        }
    }

    @Test
    fun `a module without a version is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            GatewayDependencyManifest.read("""{"format":1,"dependencies":{"g:a":""}}""")
        }
    }

    @Test
    fun `a module coordinate without a group is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            GatewayDependencyManifest.read("""{"format":1,"dependencies":{"a":"1.0"}}""")
        }
    }
}
