package top.fatweb.apimanagement.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import top.fatweb.apimanagement.api.v1.version.VersionRange
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

        ext.pluginId.convention(project.name)
        ext.pluginName.convention(ext.pluginId)
        ext.description.convention("")
        ext.author.convention("")
        ext.requiresGateway.convention(">=1.0.0 <2.0.0")
        ext.requiresGatewayApi.convention("^1.0")
        ext.serverUrl.convention(
            providers
                .gradleProperty("apiPlugin.serverUrl")
                .orElse(providers.environmentVariable("API_PLUGIN_SERVER_URL")),
        )
        ext.accessToken.convention(
            providers
                .gradleProperty("apiPlugin.accessToken")
                .orElse(providers.environmentVariable("API_PLUGIN_ACCESS_TOKEN")),
        )
        ext.trustSigner.convention(
            providers.gradleProperty("apiPlugin.trustSigner").map { it.toBoolean() }.orElse(false),
        )

        val projectVersion = providers.provider { project.version.toString() }
        val versionName =
            ext.versionName
                .orElse(projectVersion)
                .map { version -> version.ifBlank { projectVersion.get() } }

        val gatewayApiVersion =
            ext.requiresGatewayApi.map { range ->
                VersionRange.parse(range).minimumVersion().toString()
            }

        ensureRepositories(project)
        project.dependencies.addProvider(
            "compileOnly",
            gatewayApiVersion.map { "top.fatweb:api-management-gateway-api:$it" },
        )

        val gatewayApi =
            project.configurations.create("apiPluginGatewayApi") { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = true
                configuration.isTransitive = false
            }
        project.dependencies.addProvider(
            gatewayApi.name,
            gatewayApiVersion.map { "top.fatweb:api-management-gateway-api:$it" },
        )

        val generatedRoot = layout.buildDirectory.dir("generated/api-plugin")
        val modulesDirectory = layout.buildDirectory.dir("generated/api-plugin-modules")
        val keysDir = layout.projectDirectory.dir("keys")
        val libsDir = layout.buildDirectory.dir("libs")
        val runtimeClasspath = project.configurations.named("runtimeClasspath")

        val lockTask =
            project.tasks.register("generatePluginLock", GeneratePluginLock::class.java) { task ->
                task.group = "api plugin"
                task.description = "Resolves and locks the plugin's runtime dependency modules"
                task.classpath.from(runtimeClasspath)
                task.artifactModules.set(
                    providers.provider { artifactModules(runtimeClasspath.get()) },
                )
                task.dependencyEdges.set(providers.provider { dependencyEdges(runtimeClasspath.get()) })
                task.outputFile.set(generatedRoot.map { it.file("META-INF/api-plugin-deps.json") })
                task.modulesDirectory.set(modulesDirectory)
            }

        val descriptorTask =
            project.tasks.register(
                "generatePluginDescriptor",
                GeneratePluginDescriptor::class.java,
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
                task.requiresGateway.set(ext.requiresGateway)
                task.requiresGatewayApi.set(ext.requiresGatewayApi)
                task.outputFile.set(generatedRoot.map { it.file("META-INF/api-plugin.json") })
            }

        val genKeysTask =
            project.tasks.register("genPluginKeys", GenPluginKeys::class.java) { task ->
                task.group = "api plugin"
                task.description = "Generates the Ed25519 developer key pair under keys/ (reuses an existing pair)"
                task.keysDir.set(keysDir)
            }

        val pubKeyTask =
            project.tasks.register("preparePluginPubKey", PreparePluginPubKey::class.java) { task ->
                task.group = "api plugin"
                task.description = "Stages keys/public.pem as META-INF/plugin.pub.pem for the jar"
                task.dependsOn(genKeysTask)
                task.keysDir.set(keysDir)
                task.outputFile.set(generatedRoot.map { it.file("META-INF/plugin.pub.pem") })
            }

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainClasses = sourceSets.getByName("main").output.classesDirs
        val abiTask =
            project.tasks.register("checkPluginAbi", CheckPluginAbi::class.java) { task ->
                task.group = "verification"
                task.description = "Checks plugin bytecode against the gateway API ABI"
                task.classesDirs.from(mainClasses)
                task.abiJar.from(gatewayApi)
                task.dependsOn(project.tasks.named("classes"))
            }
        project.tasks.named("check").configure { task -> task.dependsOn(abiTask) }

        val archiveName =
            ext.archiveName.orElse(
                ext.pluginId.zip(versionName) { id, version -> "$id-$version.jar" },
            )
        val jarFile = archiveName.zip(libsDir) { name, dir -> dir.file(name) }

        project.tasks.named("jar", Jar::class.java).configure { jar ->
            jar.dependsOn(descriptorTask, pubKeyTask, lockTask)
            jar.archiveFileName.set(archiveName)
            jar.from(generatedRoot)
            jar.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        val signTask =
            project.tasks.register("signPlugin", SignPlugin::class.java) { task ->
                task.group = "api plugin"
                task.description = "Signs the plugin jar (Ed25519) using keys/private.pem"
                task.dependsOn(genKeysTask, project.tasks.named("jar"), abiTask)
                task.jarFile.set(jarFile)
                task.privateKeyFile.set(layout.projectDirectory.file("keys/private.pem"))
            }

        project.tasks.named("build").configure { build ->
            build.dependsOn(signTask)
        }

        val verifyTask =
            project.tasks.register("verifyPlugin", VerifyPlugin::class.java) { task ->
                task.group = "api plugin"
                task.description = "Verifies the signature embedded in the plugin jar"
                task.dependsOn(signTask)
                task.jarFile.set(jarFile)
            }

        project.tasks.register("deployPlugin", DeployPlugin::class.java) { task ->
            task.group = "api plugin"
            task.description = "Uploads the signed plugin jar to a running gateway"
            task.dependsOn(verifyTask)
            task.serverUrl.set(ext.serverUrl)
            task.accessToken.set(ext.accessToken)
            task.trustSigner.set(ext.trustSigner)
            task.jarFile.set(jarFile)
            task.modulesDirectory.set(modulesDirectory)
        }
    }

    private fun artifactModules(configuration: org.gradle.api.artifacts.Configuration): Map<String, String> =
        configuration.incoming.artifacts.artifacts.associate { artifact ->
            val module =
                componentId(artifact.id.componentIdentifier)
                    ?: "file:${artifact.file.absolutePath}"
            artifact.file.absolutePath to module
        }

    private fun dependencyEdges(configuration: org.gradle.api.artifacts.Configuration): List<String> {
        val edges = linkedSetOf<String>()
        configuration.incoming.resolutionResult.allComponents.forEach { component ->
            val from = componentId(component.id) ?: return@forEach
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
                val to = componentId(dependency.selected.id) ?: return@forEach
                edges += "$from=$to"
            }
        }
        return edges.sorted()
    }

    private fun componentId(id: ComponentIdentifier): String? = when (id) {
        is ModuleComponentIdentifier -> "${id.group}:${id.module}:${id.version}"
        is ProjectComponentIdentifier -> "project:${id.projectPath}"
        else -> null
    }

    private fun ensureRepositories(project: Project) {
        val existing =
            project.repositories
                .withType(MavenArtifactRepository::class.java)
                .map { it.url.toString() }
                .toSet()

        val centralUrls =
            setOf(
                "https://repo.maven.apache.org/maven2/",
                "https://repo1.maven.org/maven2/",
            )
        if (existing.none { url -> centralUrls.any { url.startsWith(it) } }) {
            project.repositories.mavenCentral()
        }

        val localUrl = File(System.getProperty("user.home"), ".m2/repository").toURI().toString()
        if (localUrl !in existing) {
            project.repositories.mavenLocal()
        }
    }
}
