package top.fatweb.apimanagement.gradle

import org.gradle.api.provider.Property

/**
 * The `apiPlugin { }` DSL of the plugin.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ApiPluginExtension {
    /**
     * Unique plugin ID, `^[a-z][a-z0-9-]{0,63}$` (64 characters, the width of the gateway's
     * `plugin_id` column). Defaults to the Gradle project name.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginId: Property<String>

    /**
     * Plugin display name (menus / permission tree / docs).
     * Defaults to [pluginId].
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val pluginName: Property<String>

    /**
     * Human-readable version, e.g. "1.2.0". Falls back to the project `version`
     * when unset or blank.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val versionName: Property<String>

    /**
     * Monotonically increasing integer version. **Required** — no default, so a release can
     * never silently ship with versionCode = 1.
     *
     * A version above the installed one is an upgrade. The version that is already installed
     * may be deployed again, which replaces the code in place and is what a development loop
     * does over and over - so this only has to move when a release does. Only a version below
     * the installed one is refused.
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
     * Fully-qualified class name of a [top.fatweb.apimanagement.sdk.plugin.PluginLifecycle]
     * implementation, if any.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val mainClass: Property<String>

    /**
     * `top.fatweb:api-management-plugin-sdk` version added to `implementation`.
     * Defaults to the version this plugin was released with.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sdkVersion: Property<String>

    /**
     * Optional full jar file name, e.g. "geo-1.2.0.jar".
     * Defaults to `<pluginId>-<versionName>.jar`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val archiveName: Property<String>

    /**
     * Gateway base URL that `deployPlugin` uploads to, e.g. "http://localhost:8080".
     * No default; required for a deploy.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val serverUrl: Property<String>

    /**
     * Access token from a console login, used by `deployPlugin`. No default; required for a
     * deploy. Supply it through a gradle property or an environment variable so it never
     * lands in the build script.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val accessToken: Property<String>

    /**
     * Whether `deployPlugin` registers the jar's signer public key with the gateway before
     * installing it. Defaults to false (the gateway normally already trusts the signer).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val trustSigner: Property<Boolean>

    /**
     * Whether runtime dependencies are bundled into the plugin jar as nested jars.
     * Defaults to true.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val bundleDependencies: Property<Boolean>

    /**
     * Whether the plugin bundles its own BouncyCastle instead of using the gateway's shared
     * copy. Defaults to false.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val bundleBouncyCastle: Property<Boolean>
}
