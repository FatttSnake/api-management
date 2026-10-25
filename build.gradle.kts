import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dokka)
    id("signing")
}

group = "top.fatweb"
version = "1.0.0-SNAPSHOT"
description = "api-management"

springBoot {
    buildInfo()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":plugin-sdk"))

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.data.redis)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)

    implementation(libs.bcpkix.jdk18on)

    implementation(libs.java.jwt)

    runtimeOnly(libs.mysql.connector.j)
    implementation(libs.sqlite.jdbc)
    implementation(libs.hikaricp)
    implementation(libs.dynamic.datasource)
    implementation(platform(libs.mybatis.plus.bom))
    implementation(libs.mybatis.plus)
    implementation(libs.mybatis.plus.jsqlparser)
    implementation(libs.flyway.mysql)

    implementation(libs.avatar.generator)

    implementation(libs.oshi.core.ffm)

    implementation(libs.commons.codec)

    implementation(libs.zxing.core)

    implementation(libs.aircompressor.v3)

    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)

    implementation(libs.retrofit)

    implementation(libs.swagger.annotations.jakarta)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.mybatis.plus.test)
}

fun Project.envOrProperty(name: String): Provider<String> {
    return providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_$name"))
        .map { it }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val env = envOrProperty("env").getOrElse("dev")

// A JDWP listener for attaching a debugger, off unless it is asked for: the agent is an
// unauthenticated way into this JVM, so nothing about it may be on by default. The bound
// address defaults to the loopback interface for the same reason; the container case is the
// one that has to override it, since a container's loopback is not what a published port
// reaches. `suspend=y` holds the gateway at startup until a debugger attaches, which is what
// it takes to break inside a plugin's onStart.
if (envOrProperty("debug").orNull?.toBoolean() == true) {
    val debugPort = envOrProperty("debug.port").getOrElse("5005")
    val debugAddress = envOrProperty("debug.address").getOrElse("127.0.0.1")
    val debugSuspend = envOrProperty("debug.suspend").getOrElse("n")

    tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
        jvmArgs(
            "-agentlib:jdwp=transport=dt_socket,server=y," +
                "suspend=$debugSuspend,address=$debugAddress:$debugPort"
        )
    }
}

if (env == "dev" || env == "release") {
    dependencies {
        if (env == "dev") {
            implementation(libs.knife4j.starter)
        } else {
            implementation(libs.knife4j.starter) {
                exclude(group = "com.github.xiaoymin", module = "knife4j-openapi3-ui")
                exclude(group = "org.webjars", module = "swagger-ui")
            }
        }
    }
}

tasks.register("generateChecksums") {
    description = "Generate checksums"
    dependsOn(tasks.jar)
    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val outputDir = layout.buildDirectory.dir("checksums").get().asFile
        outputDir.mkdirs()

        listOf("MD5", "SHA-1", "SHA-256", "SHA-512").forEach { algorithm ->
            val checksumFile = File(outputDir, "${jarFile.name}.${algorithm}")
            val checksum = MessageDigest.getInstance(algorithm)
                .digest(jarFile.readBytes())
                .joinToString(separator = "") { byte ->
                    "%02x".format(byte)
                }
            checksumFile.writeText("$checksum  ${jarFile.name}")
        }
    }
}

if (env == "release") {
    signing {
        val signingKey = envOrProperty("GPG_SIGNING_KEY").orNull
        val signingPassword = envOrProperty("GPG_SIGNING_PASSWORD").orNull
        if (signingKey != null) {
            useInMemoryPgpKeys(signingKey.replace("\\n", "\n"), signingPassword)
        } else {
            useGpgCmd()
        }
        sign(tasks.bootJar.get())
    }

    tasks.bootJar {
        finalizedBy("signBootJar")
    }
}

/**
 * Checks the plugin SDK's gateway dependency manifest against this gateway
 *
 * The SDK embeds the versions of the modules a plugin may only use from the gateway, and a
 * plugin build fails when it resolves any of them at another version. This task is the other
 * half: it fails when the gateway's own resolved versions move away from that manifest.
 */
abstract class VerifyGatewayDependencyManifest : DefaultTask() {
    @get:Input
    abstract val gatewayModules: MapProperty<String, String>

    @get:Input
    abstract val gatewayVersion: Property<String>

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun run() {
        val manifest = JsonSlurper().parse(manifestFile.get().asFile) as Map<*, *>
        val manifestVersion = manifest["sdkVersion"] as? String
        if (manifestVersion != gatewayVersion.get()) {
            throw GradleException(
                "Gateway dependency manifest was generated for SDK $manifestVersion, not ${gatewayVersion.get()}"
            )
        }
        val dependencies = manifest["dependencies"] as? Map<*, *>
            ?: throw GradleException("Gateway dependency manifest has no 'dependencies' object")

        val gateway = gatewayModules.get()
        val mismatches = dependencies.entries.mapNotNull { (module, version) ->
            val gatewayVersion = gateway[module] ?: return@mapNotNull null
            if (gatewayVersion == version) {
                null
            } else {
                "$module is $version in the manifest but $gatewayVersion in the gateway"
            }
        }
        if (mismatches.isNotEmpty()) {
            throw GradleException(
                "Gateway dependency manifest is stale:\n" + mismatches.joinToString("\n") { "  $it" }
            )
        }
    }
}

val verifyGatewayDependencyManifest = tasks.register<VerifyGatewayDependencyManifest>(
    "verifyGatewayDependencyManifest"
) {
    group = "verification"
    description = "Checks the plugin SDK's gateway dependency manifest against this gateway"
    gatewayVersion.set(project.version.toString())
    manifestFile.set(
        project(":plugin-sdk").layout.buildDirectory.file(
            "generated/plugin-sdk/META-INF/api-management-gateway-dependencies.json"
        )
    )
    gatewayModules.set(providers.provider {
        configurations.named("runtimeClasspath").get()
            .incoming.resolutionResult.allComponents
            .mapNotNull { component ->
                val id = component.id as? ModuleComponentIdentifier ?: return@mapNotNull null
                "${id.group}:${id.module}" to id.version
            }
            .toMap()
    })
    dependsOn(":plugin-sdk:generatePluginSdkResources")
}

tasks.named("check") {
    dependsOn(verifyGatewayDependencyManifest)
}
