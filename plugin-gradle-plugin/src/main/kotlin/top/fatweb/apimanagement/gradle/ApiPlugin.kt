package top.fatweb.apimanagement.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.bundling.Jar
import java.io.File

/**
 * The `top.fatweb.api-plugin` Gradle plugin.
 *
 * Turns a plain Kotlin/JVM project into an API Management plugin project:
 * - adds the plugin SDK to `implementation` and the needed repositories;
 * - generates `META-INF/api-plugin.json` from the `apiPlugin { }` DSL;
 * - generates the Ed25519 developer key pair (`keys/`) on first build and embeds
 *   `META-INF/plugin.pub.pem` into the jar;
 * - names the jar `<pluginId>-<versionName>.jar`;
 * - signs the jar in place (`signPlugin`, part of `build`) and provides
 *   `verifyPlugin` for self-checks.
 *
 * Requiring only the Kotlin JVM plugin, a project's build script shrinks to:
 *
 * ```kotlin
 * plugins {
 *     kotlin("jvm") version "2.3.21"
 *     id("top.fatweb.api-plugin") version "1.0.0-SNAPSHOT"
 * }
 * version = "1.0.0"
 * apiPlugin {
 *     pluginId = "geo"
 *     pluginName = "Geo 插件"
 *     versionCode = 1
 * }
 * ```
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ApiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("apiPlugin", ApiPluginExtension::class.java)
        val layout = project.layout
        val providers = project.providers

        // --- DSL defaults (all overridable) --------------------------------
        // versionCode deliberately has NO default: it drives the gateway's upgrade
        // check, so a new release must set it explicitly rather than silently
        // shipping versionCode = 1 (which the gateway rejects as a downgrade).
        ext.pluginId.convention(project.name)
        ext.pluginName.convention(ext.pluginId)
        ext.description.convention("")
        ext.author.convention("")
        ext.sdkVersion.convention(
            providers.gradleProperty("apiPlugin.sdkVersion").orElse(providers.provider { DEFAULT_SDK_VERSION })
        )

        // versionName falls back to the project `version` when it is unset OR
        // blank, so an empty string behaves exactly like "not set".
        val projectVersion = providers.provider { project.version.toString() }
        val versionName = ext.versionName
            .orElse(projectVersion)
            .map { version -> version.ifBlank { projectVersion.get() } }

        // --- repositories + SDK dependency -----------------------------------
        ensureRepositories(project)
        project.dependencies.add("implementation", providers.provider {
            "top.fatweb:api-management-plugin-sdk:${ext.sdkVersion.get()}"
        })

        // --- task wiring -------------------------------------------------------
        val generatedRoot = layout.buildDirectory.dir("generated/api-plugin")
        val keysDir = layout.projectDirectory.dir("keys")
        val libsDir = layout.buildDirectory.dir("libs")

        val descriptorTask = project.tasks.register(
            "generatePluginDescriptor",
            GeneratePluginDescriptor::class.java
        ) { task ->
            task.group = "api plugin"
            task.description = "Generates META-INF/api-plugin.json from the apiPlugin extension"
            task.pluginId.set(ext.pluginId)
            task.pluginName.set(ext.pluginName)
            task.versionName.set(versionName)
            task.versionCode.set(ext.versionCode)
            task.pluginDescription.set(ext.description)
            task.author.set(ext.author)
            task.mainClass.set(ext.mainClass)
            task.outputFile.set(generatedRoot.map { it.file("META-INF/api-plugin.json") })
        }

        val genKeysTask = project.tasks.register("genPluginKeys", GenPluginKeys::class.java) { task ->
            task.group = "api plugin"
            task.description = "Generates the Ed25519 developer key pair under keys/ (skips when present)"
            task.keysDir.set(keysDir)
        }

        val pubKeyTask = project.tasks.register("preparePluginPubKey", PreparePluginPubKey::class.java) { task ->
            task.group = "api plugin"
            task.description = "Stages keys/public.pem as META-INF/plugin.pub.pem for the jar"
            task.dependsOn(genKeysTask)
            task.keysDir.set(keysDir)
            task.outputFile.set(generatedRoot.map { it.file("META-INF/plugin.pub.pem") })
        }

        val archiveName = ext.archiveName.orElse(
            ext.pluginId.zip(versionName) { id, v -> "$id-$v.jar" }
        )
        val jarFile = archiveName.zip(libsDir) { name, dir -> dir.file(name) }

        project.tasks.named("jar", Jar::class.java).configure { jar ->
            jar.dependsOn(descriptorTask, pubKeyTask)
            jar.archiveFileName.set(archiveName)
            jar.from(generatedRoot)
        }

        val signTask = project.tasks.register("signPlugin", SignPlugin::class.java) { task ->
            task.group = "api plugin"
            task.description = "Signs the plugin jar (Ed25519) using keys/private.pem"
            task.dependsOn(genKeysTask, project.tasks.named("jar"))
            task.jarFile.set(jarFile)
            task.privateKeyFile.set(layout.projectDirectory.file("keys/private.pem"))
        }

        // `./gradlew build` produces an already-signed jar.
        project.tasks.named("build").configure { build ->
            build.dependsOn(signTask)
        }

        project.tasks.register("verifyPlugin", VerifyPlugin::class.java) { task ->
            task.group = "api plugin"
            task.description = "Verifies the signature embedded in the plugin jar"
            task.dependsOn(signTask)
            task.jarFile.set(jarFile)
        }
    }

    private fun ensureRepositories(project: Project) {
        val existing = project.repositories
            .withType(MavenArtifactRepository::class.java)
            .map { it.url.toString() }
            .toSet()

        val centralUrls = setOf(
            "https://repo.maven.apache.org/maven2/",
            "https://repo1.maven.org/maven2/"
        )
        if (existing.none { url -> centralUrls.any { url.startsWith(it) } }) {
            project.repositories.mavenCentral()
        }

        val localUrl = File(System.getProperty("user.home"), ".m2/repository").toURI().toString()
        if (localUrl !in existing) {
            project.repositories.mavenLocal()
        }
    }

    companion object {
        private const val DEFAULT_SDK_VERSION = "1.0.0-SNAPSHOT"
    }
}
