import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.Category
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "top.fatweb"
version = "1.0.0-SNAPSHOT"
description = "api-management plugin sdk"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot dependency management so Spring resolves to the same major
    // version as the gateway (identity of @RestController/@RequestMapping must match).
    api(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    api("org.springframework:spring-web")
    // @Service / @Component / @Autowired for plugin beans (spring-web does not
    // transitively expose spring-context's org.springframework.stereotype package).
    api("org.springframework:spring-context")
    // @NotNull / @NotBlank / @Valid on plugin DTOs. Only the API is exposed: the gateway
    // already runs Hibernate Validator, and `jakarta.` classes resolve parent-first.
    api("jakarta.validation:jakarta.validation-api")
    // Logging: `org.slf4j.` is a parent-first prefix in PluginClassLoaderManager, so at
    // runtime the plugin gets the gateway's binding and appenders. Exposing the API here
    // saves every plugin from declaring it just to compile.
    api("org.slf4j:slf4j-api")
    // Jackson 3 with the Kotlin module, for `jacksonObjectMapper()` and the `readValue<T>()`
    // extensions. `tools.jackson.` is a parent-first prefix for the same reason.
    api(libs.jackson.module.kotlin)
    api(libs.swagger.annotations.jakarta)
    // Flyway is provided by the gateway: a plugin that migrates its own datasource compiles
    // against this, and the bundler strips it from the plugin jar. The version is pinned here
    // as well as in the gateway, and checkPluginClasspath keeps the two from drifting apart.
    api(libs.flyway.core)

    testImplementation(libs.kotlin.test.junit5)
}

// The gateway's provided dependency surface, resolved against the same BOMs the gateway uses.
// This is not published; it only feeds the manifest a plugin build is checked against.
val gatewayProvidedDependencies = configurations.create("gatewayProvidedDependencies") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// The gateway adds Knife4j (and so Springdoc's Jackson 2) in dev and release builds; the
// manifest has to describe the classpath a plugin will actually meet
val env = providers.gradleProperty("env")
    .orElse(providers.environmentVariable("env"))
    .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_env"))
    .getOrElse("dev")

dependencies {
    add(gatewayProvidedDependencies.name, platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    add(gatewayProvidedDependencies.name, platform(libs.mybatis.plus.bom))
    add(gatewayProvidedDependencies.name, libs.spring.boot.starter.web)
    add(gatewayProvidedDependencies.name, libs.spring.boot.starter.validation)
    add(gatewayProvidedDependencies.name, libs.spring.boot.starter.security)
    add(gatewayProvidedDependencies.name, libs.spring.boot.starter.mail)
    add(gatewayProvidedDependencies.name, libs.spring.boot.starter.data.redis)
    add(gatewayProvidedDependencies.name, libs.jackson.module.kotlin)
    add(gatewayProvidedDependencies.name, libs.jackson.dataformat.yaml)
    add(gatewayProvidedDependencies.name, libs.swagger.annotations.jakarta)
    add(gatewayProvidedDependencies.name, libs.flyway.mysql)
    add(gatewayProvidedDependencies.name, libs.bcpkix.jdk18on)
    add(gatewayProvidedDependencies.name, libs.hikaricp)
    add(gatewayProvidedDependencies.name, libs.dynamic.datasource)
    add(gatewayProvidedDependencies.name, libs.mybatis.plus)
    add(gatewayProvidedDependencies.name, libs.sqlite.jdbc)
    add(gatewayProvidedDependencies.name, libs.mysql.connector.j)
    add(gatewayProvidedDependencies.name, libs.kotlin.reflect)
    add(gatewayProvidedDependencies.name, libs.kotlinx.coroutines.core)
    add(gatewayProvidedDependencies.name, "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    // Not provided itself, but it is what pulls the gateway's Jackson 2 BOM above the one
    // Spring Boot manages, so leaving it out would model provided Jackson 2 at the wrong version
    add(gatewayProvidedDependencies.name, libs.java.jwt)
    if (env == "dev" || env == "release") {
        add(gatewayProvidedDependencies.name, libs.knife4j.starter)
    }
}

/**
 * Writes the SDK's generated resources: its version, and the versions of the modules the
 * gateway provides (keyed by `group:name`).
 */
abstract class GeneratePluginSdkResources : DefaultTask() {
    @get:Input
    abstract val versions: MapProperty<String, String>

    @get:Input
    abstract val sdkVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val root = outputDirectory.get().asFile
        root.deleteRecursively()
        val metaInf = root.resolve("META-INF")
        metaInf.mkdirs()

        val payload = mapOf(
            "format" to 1,
            "sdkVersion" to sdkVersion.get(),
            "dependencies" to versions.get().toSortedMap()
        )
        metaInf.resolve("api-management-gateway-dependencies.json").writeText(JsonOutput.toJson(payload))
        metaInf.resolve("api-management-plugin-sdk.properties").writeText("version=${sdkVersion.get()}\n")
    }
}

private fun isPlatform(component: ResolvedComponentResult): Boolean {
    val module = (component.id as? ModuleComponentIdentifier)?.module
    if (module != null && (module.endsWith("-bom") || module == "spring-boot-dependencies")) {
        return true
    }
    return component.variants.any { variant ->
        val category = variant.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name
        category == Category.REGULAR_PLATFORM || category == Category.ENFORCED_PLATFORM
    }
}

val generatePluginSdkResources = tasks.register<GeneratePluginSdkResources>("generatePluginSdkResources") {
    group = "api plugin"
    description = "Writes the SDK version and gateway-provided dependency manifest embedded in the plugin SDK"
    sdkVersion.set(project.version.toString())
    outputDirectory.set(layout.buildDirectory.dir("generated/plugin-sdk"))
    versions.set(providers.provider {
        gatewayProvidedDependencies.incoming.resolutionResult.allComponents
            .filterNot(::isPlatform)
            .mapNotNull { component ->
                val id = component.id as? ModuleComponentIdentifier ?: return@mapNotNull null
                "${id.group}:${id.module}" to id.version
            }
            .toMap()
    })
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets.named("main") {
    resources.srcDir(generatePluginSdkResources.flatMap { it.outputDirectory })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "api-management-plugin-sdk"
            pom {
                name.set("api-management-plugin-sdk")
                description.set("API management gateway plugin SDK")
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
            val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}
