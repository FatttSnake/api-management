package top.fatweb.apimanagement.gradle

import org.gradle.api.provider.Property

/**
 * The `apiPlugin { }` DSL of the plugin.
 *
 * Sensible defaults exist for most fields: [pluginId] falls back to the Gradle
 * project name, [pluginName] to the plugin id, [versionName] to the project
 * version, and [sdkVersion] to the SDK version the plugin was built against.
 * [versionCode] is deliberately **required** (no default) — it drives the
 * gateway's upgrade check, so an unset value must fail loudly instead of
 * silently shipping versionCode = 1.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ApiPluginExtension {
    /**
     * Unique plugin ID, `^[a-z][a-z0-9-]*$`; must match `@ApiController.plugin`
     * Defaults to the Gradle project name.
     */
    val pluginId: Property<String>

    /**
     * Plugin display name (menus / permission tree / docs).
     * Defaults to [pluginId].
     */
    val pluginName: Property<String>

    /**
     * Human-readable version, e.g. "1.2.0". Falls back to the project `version`
     * when unset or blank.
     */
    val versionName: Property<String>

    /**
     * Monotonically increasing integer version; upgrades must exceed the
     * currently installed one. **Required** — no default, so a release can never
     * silently ship with versionCode = 1; bump it on every upgrade.
     */
    val versionCode: Property<Int>

    /**
     * Plugin description.
     */
    val description: Property<String>

    /**
     * Plugin author.
     */
    val author: Property<String>

    /**
     * Fully-qualified class name of a [top.fatweb.apimanagement.sdk.plugin.PluginLifecycle]
     * implementation, if any.
     */
    val mainClass: Property<String>

    /**
     * `top.fatweb:api-management-plugin-sdk` version added to `implementation`.
     * Defaults to the version this plugin was released with.
     */
    val sdkVersion: Property<String>

    /**
     * Optional full jar file name, e.g. "geo-1.2.0.jar".
     * Defaults to `<pluginId>-<versionName>.jar`.
     */
    val archiveName: Property<String>
}
