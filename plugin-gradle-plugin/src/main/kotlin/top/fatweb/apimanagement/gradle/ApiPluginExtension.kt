package top.fatweb.apimanagement.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * The `apiPlugin { }` DSL of a plugin project.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ApiPluginExtension {
    /**
     * Unique plugin ID, `^[a-z][a-z0-9-]{0,63}$`. Defaults to the Gradle project name.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: Property<String>

    /**
     * Plugin display name. Defaults to [pluginId].
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginName: Property<String>

    /**
     * Human-readable version. Falls back to the project version.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionName: Property<String>

    /**
     * Monotonically increasing integer version. Required.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionCode: Property<Int>

    /**
     * Plugin description.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val description: Property<String>

    /**
     * Plugin author.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val author: Property<String>

    /**
     * Fully-qualified class name of the [top.fatweb.apimanagement.api.v1.ApiPlugin] implementation.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val mainClass: Property<String>

    /**
     * Gateway version range required by the plugin. Defaults to `>=1.0.0 <2.0.0`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val requiresGateway: Property<String>

    /**
     * Gateway API version range required by the plugin. Defaults to `^1.0`.
     *
     * The minimum version is also the gateway-api artifact used to compile the plugin, and its
     * major version becomes the plugin's API major.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val requiresGatewayApi: Property<String>

    /**
     * Optional full jar file name. Defaults to `<pluginId>-<versionName>.jar`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val archiveName: Property<String>

    /**
     * Gateway base URL used by `deployPlugin`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val serverUrl: Property<String>

    /**
     * Console access token used by `deployPlugin`. Supply it through a Gradle property or an
     * environment variable.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val accessToken: Property<String>

    /**
     * Whether `deployPlugin` registers the signer public key before installing.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val trustSigner: Property<Boolean>

    /**
     * Groups of modules that share one class loader. Each entry is a list of `group:artifact`
     * values without versions.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sharedLoaders: ListProperty<List<String>>

    /**
     * Declares modules that must be loaded by one class loader.
     *
     * Use it for libraries that resolve types through `ServiceLoader` or another static
     * registry, or whose POM marks a required dependency optional.
     *
     * @param coordinates `group:artifact` values, at least two
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sharedLoader(vararg coordinates: String) {
        require(coordinates.size >= 2) { "sharedLoader requires at least two coordinates" }
        require(coordinates.all { it.isNotBlank() && it.count { character -> character == ':' } == 1 }) {
            "sharedLoader expects group:artifact without a version"
        }
        sharedLoaders.add(coordinates.toList())
    }
}
