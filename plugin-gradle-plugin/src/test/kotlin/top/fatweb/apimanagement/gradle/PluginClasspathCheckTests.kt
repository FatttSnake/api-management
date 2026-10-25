package top.fatweb.apimanagement.gradle

import top.fatweb.apimanagement.sdk.plugin.PluginClasspath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plugin classpath check tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginClasspathCheck
 */
class PluginClasspathCheckTests {
    private val provided = listOf("org.flywaydb*", "org.bouncycastle*")

    @Test
    fun `a matching provided version passes`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.flywaydb:flyway-core:13.3.0"),
            runtimeModules = listOf("org.flywaydb:flyway-core:13.3.0"),
            gatewayVersions = mapOf("org.flywaydb:flyway-core" to "13.3.0"),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertTrue(result.mismatches.isEmpty())
        assertTrue(result.untracked.isEmpty())
    }

    @Test
    fun `a different provided version is a mismatch`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.flywaydb:flyway-core:14.0.0"),
            runtimeModules = emptyList(),
            gatewayVersions = mapOf("org.flywaydb:flyway-core" to "13.3.0"),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertEquals(
            listOf(PluginClasspathCheck.Mismatch("org.flywaydb:flyway-core", "14.0.0", "13.3.0")),
            result.mismatches
        )
    }

    @Test
    fun `an older provided version is allowed`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.flywaydb:flyway-core:12.0.0"),
            runtimeModules = emptyList(),
            gatewayVersions = mapOf("org.flywaydb:flyway-core" to "13.3.0"),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertTrue(result.mismatches.isEmpty())
    }

    @Test
    fun `a different non-provided version is ignored`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("com.example:library:1.0"),
            runtimeModules = emptyList(),
            gatewayVersions = mapOf("com.example:library" to "2.0"),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertTrue(result.mismatches.isEmpty())
        assertTrue(result.untracked.isEmpty())
    }

    @Test
    fun `project and file coordinates are ignored`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("project::other", "file:/tmp/thing.jar"),
            runtimeModules = emptyList(),
            gatewayVersions = emptyMap(),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertTrue(result.mismatches.isEmpty())
        assertTrue(result.untracked.isEmpty())
    }

    @Test
    fun `a bundled BouncyCastle is not checked`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.bouncycastle:bcprov-jdk18on:1.90"),
            runtimeModules = emptyList(),
            gatewayVersions = mapOf("org.bouncycastle:bcprov-jdk18on" to "1.86"),
            providedPatterns = provided,
            bundleBouncyCastle = true
        )

        assertTrue(result.mismatches.isEmpty())
        assertTrue(result.untracked.isEmpty())
    }

    @Test
    fun `a provided module the manifest does not list is untracked`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.flywaydb:flyway-core:13.3.0"),
            runtimeModules = emptyList(),
            gatewayVersions = emptyMap(),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertEquals(listOf("org.flywaydb:flyway-core"), result.untracked)
    }

    @Test
    fun `the SDK itself is not reported`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("top.fatweb:api-management-plugin-sdk:1.0.0"),
            runtimeModules = emptyList(),
            gatewayVersions = emptyMap(),
            providedPatterns = listOf(*PluginClasspath.PROVIDED_DEPENDENCIES.toTypedArray()),
            bundleBouncyCastle = false
        )

        assertTrue(result.mismatches.isEmpty())
        assertTrue(result.untracked.isEmpty())
    }

    @Test
    fun `a module resolved differently on the two classpaths is reported`() {
        val result = PluginClasspathCheck.check(
            compileModules = listOf("org.flywaydb:flyway-core:13.3.0"),
            runtimeModules = listOf("org.flywaydb:flyway-core:14.0.0"),
            gatewayVersions = mapOf("org.flywaydb:flyway-core" to "13.3.0"),
            providedPatterns = provided,
            bundleBouncyCastle = false
        )

        assertEquals(
            listOf(PluginClasspathCheck.Mismatch("org.flywaydb:flyway-core", "14.0.0", "13.3.0")),
            result.mismatches
        )
    }
}
