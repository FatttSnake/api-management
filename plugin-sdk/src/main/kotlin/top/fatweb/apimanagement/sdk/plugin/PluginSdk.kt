package top.fatweb.apimanagement.sdk.plugin

import java.util.Properties

/**
 * Plugin SDK version and version comparison
 *
 * The version is baked into the SDK jar at build time. A plugin records the version it was
 * built with in its descriptor, and the gateway refuses one built against a newer SDK than
 * its own.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginSdk {
    /**
     * Classpath resource carrying the SDK version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val VERSION_RESOURCE = "/META-INF/api-management-plugin-sdk.properties"

    /**
     * Version of the SDK this code was loaded from
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val version: String by lazy {
        val stream = PluginSdk::class.java.getResourceAsStream(VERSION_RESOURCE)
            ?: error("Missing $VERSION_RESOURCE in the plugin SDK")
        val properties = Properties()
        stream.use { properties.load(it) }
        properties.getProperty("version")?.takeIf { it.isNotBlank() }
            ?: error("Missing 'version' in $VERSION_RESOURCE")
    }

    /**
     * Whether a version is newer than another
     *
     * Compares the numeric core, padding the shorter one with zeroes; when the cores are
     * equal, a release is newer than a pre-release. Qualifier ordering beyond that (two
     * different pre-release labels) is not modelled.
     *
     * @param candidate Version to test
     * @param reference Version to test against
     * @return True when [candidate] is strictly newer
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isNewer(candidate: String, reference: String = version): Boolean {
        fun core(value: String): List<Int> =
            value.substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }

        val candidateCore = core(candidate)
        val referenceCore = core(reference)
        repeat(maxOf(candidateCore.size, referenceCore.size)) { index ->
            val left = candidateCore.getOrElse(index) { 0 }
            val right = referenceCore.getOrElse(index) { 0 }
            if (left != right) {
                return left > right
            }
        }

        val candidateQualifier = candidate.substringAfter('-', "")
        val referenceQualifier = reference.substringAfter('-', "")
        return candidateQualifier.isEmpty() && referenceQualifier.isNotEmpty()
    }
}
