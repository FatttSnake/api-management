package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import top.fatweb.apimanagement.sdk.plugin.GatewayDependencyManifest
import top.fatweb.apimanagement.sdk.plugin.PluginClasspath
import top.fatweb.apimanagement.sdk.plugin.PluginSdk
import java.util.jar.JarFile

/**
 * Checks that gateway-provided dependencies resolve to the versions the gateway ships
 *
 * The gateway always loads its own classes for those modules, so a plugin compiled against a
 * newer version fails at mount time rather than at compile time. This task turns that into a
 * build failure by comparing both the compile and the runtime classpath with the manifest
 * embedded in the plugin SDK. Older versions are allowed: the SDK only grows.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Compares resolved classpaths; cheap and not worth caching")
abstract class CheckPluginClasspath : DefaultTask() {
    /**
     * The plugin SDK jar carrying the manifest
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:InputFiles
    @get:Classpath
    abstract val sdkJar: ConfigurableFileCollection

    /**
     * Compile classpath entries, path to `group:name:version`
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val compileModules: MapProperty<String, String>

    /**
     * Runtime classpath entries, path to `group:name:version`
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val runtimeModules: MapProperty<String, String>

    /**
     * Gateway-provided dependency patterns
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val providedPatterns: ListProperty<String>

    /**
     * Whether the plugin bundles its own BouncyCastle, which is then deliberately not provided
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val bundleBouncyCastle: Property<Boolean>

    @TaskAction
    fun run() {
        val jar = sdkJar.files.singleOrNull { it.isFile }
            ?: throw GradleException(
                "Cannot locate the plugin SDK jar among ${sdkJar.files}. The SDK carries the gateway's " +
                    "dependency versions, and this check needs it."
            )
        val json = JarFile(jar).use { file ->
            val entry = file.getJarEntry(GatewayDependencyManifest.RESOURCE)
                ?: throw GradleException(
                    "${jar.name} does not carry ${GatewayDependencyManifest.RESOURCE}. " +
                        "Use a plugin SDK version that ships the gateway dependency manifest."
                )
            file.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
        }
        val manifest = GatewayDependencyManifest.read(json)

        val result = PluginClasspathCheck.check(
            compileModules = compileModules.get().values,
            runtimeModules = runtimeModules.get().values,
            gatewayVersions = manifest.dependencies,
            providedPatterns = providedPatterns.get(),
            bundleBouncyCastle = bundleBouncyCastle.get()
        )

        result.untracked.forEach { module ->
            logger.warn(
                "Gateway-provided dependency {} is not tracked by the SDK manifest; its version cannot be verified",
                module
            )
        }
        if (result.mismatches.isNotEmpty()) {
            val details = result.mismatches.joinToString("\n") {
                "  ${it.module}: ${it.version} (gateway ${it.gatewayVersion})"
            }
            throw GradleException(
                "Gateway-provided dependencies resolve to versions newer than the gateway ships:\n" +
                    details +
                    "\nThe gateway always loads its own copy at runtime. Declare these with " +
                    "compileOnly(\"group:name\") so the SDK pins the version, or align them with the gateway."
            )
        }
    }
}

/**
 * Version comparison behind [CheckPluginClasspath], kept apart so it can be tested
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal object PluginClasspathCheck {
    /**
     * One provided module resolved at a version newer than the gateway's
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Mismatch(
        /**
         * Module coordinate without its version, e.g. `org.flywaydb:flyway-core`
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val module: String,

        /**
         * Version the plugin resolves
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val version: String,

        /**
         * Version the gateway provides
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val gatewayVersion: String
    )

    /**
     * Outcome of the comparison
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class Result(
        /**
         * Provided modules resolved at a version newer than the gateway's
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val mismatches: List<Mismatch>,

        /**
         * Provided modules the manifest does not list
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val untracked: List<String>
    )

    private const val SDK_MODULE = "top.fatweb:api-management-plugin-sdk"

    /**
     * Compare resolved modules with the manifest
     *
     * @param compileModules Compile classpath coordinates
     * @param runtimeModules Runtime classpath coordinates
     * @param gatewayVersions Provided versions from the manifest
     * @param providedPatterns Gateway-provided dependency patterns
     * @param bundleBouncyCastle Whether BouncyCastle is deliberately bundled
     * @return Mismatches and untracked modules
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun check(
        compileModules: Collection<String>,
        runtimeModules: Collection<String>,
        gatewayVersions: Map<String, String>,
        providedPatterns: List<String>,
        bundleBouncyCastle: Boolean
    ): Result {
        val modules = (compileModules + runtimeModules)
            .mapNotNull(::splitCoordinate)
            .filter { (group, name, _) -> providedPatterns.any { PluginClasspath.matches(it, group, name) } }
            .filterNot { (group, _, _) -> bundleBouncyCastle && group == PluginClasspath.BOUNCY_CASTLE_GROUP }
            .map { (group, name, version) -> "$group:$name" to version }
            .filterNot { (module, _) -> module == SDK_MODULE }
            .distinct()

        // Asymmetric on purpose: compiling against something older than the gateway's copy is
        // normally harmless, while compiling against a newer API fails at runtime with a
        // linkage error. A lower version is therefore left alone rather than reported.
        val mismatches = modules.mapNotNull { (module, version) ->
            val gatewayVersion = gatewayVersions[module] ?: return@mapNotNull null
            if (PluginSdk.isNewer(version, gatewayVersion)) Mismatch(module, version, gatewayVersion) else null
        }.distinct().sortedBy { it.module }

        val untracked = modules.map { (module, _) -> module }
            .filterNot { it in gatewayVersions }
            .distinct()
            .sorted()

        return Result(mismatches, untracked)
    }

    private fun splitCoordinate(coordinate: String): Triple<String, String, String>? {
        if (coordinate.startsWith("project:") || coordinate.startsWith("file:")) {
            return null
        }
        val parts = coordinate.split(':')
        if (parts.size < 3) {
            return null
        }
        return Triple(parts[0], parts[1], parts[2])
    }
}
