package top.fatweb.apimanagement.sdk.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin classpath contract tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginClasspath
 */
class PluginClasspathTests {
    @Test
    fun `gateway-provided modules are recognized`() {
        assertTrue(PluginClasspath.isProvided("top.fatweb", "api-management-plugin-sdk"))
        assertTrue(PluginClasspath.isProvided("org.springframework", "spring-web"))
        assertTrue(PluginClasspath.isProvided("tools.jackson.core", "jackson-databind"))
        assertTrue(PluginClasspath.isProvided("io.micrometer", "micrometer-observation"))
        assertTrue(PluginClasspath.isProvided("commons-logging", "commons-logging"))
        assertTrue(PluginClasspath.isProvided("org.bouncycastle", "bcpkix-jdk18on"))
    }

    @Test
    fun `mybatis and mybatis-plus are provided by the gateway`() {
        assertTrue(PluginClasspath.isProvided("org.mybatis", "mybatis"))
        assertTrue(PluginClasspath.isProvided("org.mybatis", "mybatis-spring"))
        assertTrue(PluginClasspath.isProvided("com.baomidou", "mybatis-plus-core"))
        assertTrue(PluginClasspath.isProvided("com.github.jsqlparser", "jsqlparser"))
    }

    @Test
    fun `mybatis classes resolve from the gateway`() {
        assertTrue(PluginClasspath.PARENT_FIRST_PREFIXES.any {
            "org.apache.ibatis.session.SqlSessionFactory".startsWith(it)
        })
        assertTrue(PluginClasspath.PARENT_FIRST_PREFIXES.any {
            "org.mybatis.spring.SqlSessionTemplate".startsWith(it)
        })
        assertTrue(PluginClasspath.PARENT_FIRST_PREFIXES.any {
            "net.sf.jsqlparser.parser.CCJSqlParserUtil".startsWith(it)
        })
    }

    @Test
    fun `ordinary libraries are not treated as provided`() {
        assertFalse(PluginClasspath.isProvided("org.apache.commons", "commons-lang3"))
        assertFalse(PluginClasspath.isProvided("com.google.guava", "guava"))
    }

    @Test
    fun `a wildcard matches across the group and name separator`() {
        assertTrue(PluginClasspath.matches("org.springframework*", "org.springframework", "spring-core"))
        assertTrue(PluginClasspath.matches("org.jetbrains:annotations", "org.jetbrains", "annotations"))
        assertFalse(PluginClasspath.matches("org.jetbrains:annotations", "org.jetbrains", "kotlin-stdlib"))
    }
}
