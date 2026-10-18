package top.fatweb.apimanagement.sdk.plugin

/**
 * Classpath contract shared by the gateway and the plugin authoring Gradle plugin
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginClasspath {
    /**
     * Class name prefixes always resolved from the gateway class loader
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val PARENT_FIRST_PREFIXES: List<String> = listOf(
        "java.",
        "javax.",
        "jakarta.",
        "sun.",
        "jdk.",
        "org.springframework.",
        "org.apache.commons.logging.",
        "io.micrometer.",
        "org.jspecify.",
        "org.slf4j.",
        "tools.jackson.",
        "com.fasterxml.",
        "io.swagger.",
        "org.springdoc.",
        "com.baomidou.",
        "com.zaxxer.hikari.",
        "com.mysql.cj.jdbc.",
        "org.sqlite.",
        "kotlin.",
        "kotlinx.",
        "top.fatweb.apimanagement."
    )

    /**
     * BouncyCastle class name prefix
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val BOUNCY_CASTLE_PREFIX = "org.bouncycastle."

    /**
     * BouncyCastle group ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val BOUNCY_CASTLE_GROUP = "org.bouncycastle"

    /**
     * Dependencies the gateway provides, matched against `group:name`
     *
     * A `*` matches any characters, including the `:` separator. BC is provided by
     * default; a plugin that opts into bundling it removes that pattern on the build side.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val PROVIDED_DEPENDENCIES: List<String> = listOf(
        "top.fatweb:api-management-plugin-sdk",
        "org.springframework*",
        "org.springdoc*",
        "io.swagger*",
        "org.slf4j*",
        "ch.qos.logback*",
        "io.micrometer*",
        "commons-logging*",
        "org.jspecify*",
        "tools.jackson*",
        "com.fasterxml*",
        "org.jetbrains.kotlin*",
        "org.jetbrains.kotlinx*",
        "org.jetbrains:annotations",
        "jakarta*",
        "org.apache.tomcat*",
        "com.baomidou*",
        "com.zaxxer*",
        "com.mysql*",
        "org.xerial*",
        "org.flywaydb*",
        "org.bouncycastle*"
    )

    private val providedRegexes: List<Regex> = PROVIDED_DEPENDENCIES.map(::globToRegex)

    /**
     * Whether the gateway provides a module
     *
     * @param group Module group
     * @param name Module name
     * @return True when the module is provided by the gateway
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isProvided(group: String, name: String): Boolean {
        val coordinate = "$group:$name"
        return providedRegexes.any { it.matches(coordinate) }
    }

    /**
     * Whether a dependency pattern matches a module
     *
     * @param pattern Pattern from [PROVIDED_DEPENDENCIES]
     * @param group Module group
     * @param name Module name
     * @return True when the pattern matches
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun matches(pattern: String, group: String, name: String): Boolean =
        globToRegex(pattern).matches("$group:$name")

    private fun globToRegex(pattern: String): Regex =
        Regex("^" + pattern.split("*").joinToString(".*") { Regex.escape(it) } + "$")
}
