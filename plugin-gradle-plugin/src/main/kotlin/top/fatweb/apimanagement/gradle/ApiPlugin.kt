package top.fatweb.apimanagement.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import top.fatweb.apimanagement.sdk.plugin.PluginClasspath
import java.io.File

/**
 * The `top.fatweb.api-plugin` Gradle plugin.
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

        // Deploy credentials are a gradle property or an environment variable, never a build
        // script literal or a file: the access token is a shared, long-lived secret.
        ext.serverUrl.convention(
            providers.gradleProperty("apiPlugin.serverUrl")
                .orElse(providers.environmentVariable("API_PLUGIN_SERVER_URL"))
        )
        ext.accessToken.convention(
            providers.gradleProperty("apiPlugin.accessToken")
                .orElse(providers.environmentVariable("API_PLUGIN_ACCESS_TOKEN"))
        )
        ext.trustSigner.convention(
            providers.gradleProperty("apiPlugin.trustSigner").map { it.toBoolean() }.orElse(false)
        )
        ext.bundleDependencies.convention(true)
        ext.bundleBouncyCastle.convention(false)

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
            task.description = "Generates the Ed25519 developer key pair under keys/ (reuses an existing pair)"
            task.keysDir.set(keysDir)
        }

        val pubKeyTask = project.tasks.register("preparePluginPubKey", PreparePluginPubKey::class.java) { task ->
            task.group = "api plugin"
            task.description = "Stages keys/public.pem as META-INF/plugin.pub.pem for the jar"
            task.dependsOn(genKeysTask)
            task.keysDir.set(keysDir)
            task.outputFile.set(generatedRoot.map { it.file("META-INF/plugin.pub.pem") })
        }

        val runtimeClasspath = project.configurations.named("runtimeClasspath")
        val bundleLibrariesTask = project.tasks.register(
            "bundlePluginLibraries",
            BundlePluginLibraries::class.java
        ) { task ->
            task.group = "api plugin"
            task.description = "Bundles the plugin's runtime dependencies as nested jars"
            task.classpath.from(runtimeClasspath)
            task.artifactModules.set(providers.provider {
                runtimeClasspath.get().incoming.artifacts.artifacts
                    .associate { artifact ->
                        val module = componentId(artifact.id.componentIdentifier)
                            ?: "file:${artifact.file.absolutePath}"
                        artifact.file.absolutePath to module
                    }
            })
            task.dependencyEdges.set(providers.provider {
                val edges = linkedSetOf<String>()
                runtimeClasspath.get().incoming.resolutionResult.allComponents.forEach { component ->
                    val from = componentId(component.id) ?: return@forEach
                    component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
                        val to = componentId(dependency.selected.id) ?: return@forEach
                        edges += "$from=$to"
                    }
                }
                edges.sorted()
            })
            task.providedPatterns.set(ext.bundleBouncyCastle.map { bundle ->
                if (bundle) {
                    PluginClasspath.PROVIDED_DEPENDENCIES.filterNot { it == "org.bouncycastle*" }
                } else {
                    PluginClasspath.PROVIDED_DEPENDENCIES
                }
            })
            task.bundleDependencies.set(ext.bundleDependencies)
            task.bundleBouncyCastle.set(ext.bundleBouncyCastle)
            task.outputDirectory.set(layout.buildDirectory.dir("generated/api-plugin-libs"))
        }

        val archiveName = ext.archiveName.orElse(
            ext.pluginId.zip(versionName) { id, v -> "$id-$v.jar" }
        )
        val jarFile = archiveName.zip(libsDir) { name, dir -> dir.file(name) }

        project.tasks.named("jar", Jar::class.java).configure { jar ->
            jar.dependsOn(descriptorTask, pubKeyTask, bundleLibrariesTask)
            jar.archiveFileName.set(archiveName)
            jar.from(generatedRoot)
            jar.from(bundleLibrariesTask.flatMap { it.outputDirectory })
            jar.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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

        val verifyTask = project.tasks.register("verifyPlugin", VerifyPlugin::class.java) { task ->
            task.group = "api plugin"
            task.description = "Verifies the signature embedded in the plugin jar"
            task.dependsOn(signTask)
            task.jarFile.set(jarFile)
        }

        project.tasks.register("deployPlugin", DeployPlugin::class.java) { task ->
            task.group = "api plugin"
            task.description = "Uploads the signed plugin jar to a running gateway"
            // Through verifyTask, this also depends on signTask, so a deploy can never ship a
            // jar the gateway would reject as tampered with.
            task.dependsOn(verifyTask)
            task.serverUrl.set(ext.serverUrl)
            task.accessToken.set(ext.accessToken)
            task.trustSigner.set(ext.trustSigner)
            task.jarFile.set(jarFile)
        }
    }

    private fun componentId(id: ComponentIdentifier): String? = when (id) {
        is ModuleComponentIdentifier -> "${id.group}:${id.module}:${id.version}"
        is ProjectComponentIdentifier -> "project:${id.projectPath}"
        else -> null
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
