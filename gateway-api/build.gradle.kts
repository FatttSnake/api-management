import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    `java-library`
    `maven-publish`
}

group = "top.fatweb"
version = "1.0.0-SNAPSHOT"
description = "api-management gateway plugin API"

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
    api(kotlin("stdlib"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-jvm-default=no-compatibility")
    }
}

/**
 * Writes the public ABI of the gateway API jar.
 *
 * The plugin Gradle plugin and the gateway both compare a plugin's bytecode against this
 * file, so it is generated from the compiled classes rather than maintained by hand.
 */
abstract class GenerateGatewayApiAbi : DefaultTask() {
    @get:InputDirectory
    abstract val classesDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val versionFile: RegularFileProperty

    @get:Input
    abstract val apiVersion: Property<String>

    @TaskAction
    fun run() {
        val root = classesDirectory.get().asFile
        val loader = URLClassLoader(arrayOf(root.toURI().toURL()), GenerateGatewayApiAbi::class.java.classLoader)
        loader.use { loader ->
            val classes =
                root
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".class") }
                    .map {
                        it
                            .relativeTo(root)
                            .invariantSeparatorsPath
                            .removeSuffix(".class")
                            .replace('/', '.')
                    }.filterNot { it == "module-info" || it.endsWith("package-info") }
                    .sorted()
                    .mapNotNull { name ->
                        val type = Class.forName(name, false, loader)
                        if (!Modifier.isPublic(type.modifiers)) {
                            return@mapNotNull null
                        }
                        mapOf(
                            "name" to type.name,
                            "kind" to kindOf(type),
                            "constructors" to
                                type.declaredConstructors
                                    .filter { isVisible(it.modifiers) && !it.isSynthetic }
                                    .map { constructorDescriptor(it.parameterTypes) }
                                    .sorted(),
                            "methods" to
                                type.declaredMethods
                                    .filter { isVisible(it.modifiers) && !it.isSynthetic && !it.isBridge }
                                    .map { method ->
                                        mapOf(
                                            "name" to method.name,
                                            "descriptor" to methodDescriptor(method),
                                            "generic" to method.toGenericString(),
                                            "static" to Modifier.isStatic(method.modifiers),
                                            "abstract" to Modifier.isAbstract(method.modifiers),
                                        )
                                    }.sortedWith(
                                        compareBy(
                                            { it.getValue("name") as String },
                                            { it.getValue("descriptor") as String },
                                        ),
                                    ),
                            "fields" to
                                type.declaredFields
                                    .filter { isVisible(it.modifiers) && !it.isSynthetic }
                                    .map { field ->
                                        mapOf(
                                            "name" to field.name,
                                            "descriptor" to field.type.descriptorString(),
                                            "static" to Modifier.isStatic(field.modifiers),
                                            "final" to Modifier.isFinal(field.modifiers),
                                        )
                                    }.sortedBy { it.getValue("name") as String },
                        )
                    }.toList()

            val output = outputFile.get().asFile
            output.parentFile.mkdirs()
            output.writeText(
                JsonOutput.prettyPrint(
                    JsonOutput.toJson(
                        mapOf(
                            "format" to 1,
                            "artifact" to "top.fatweb:api-management-gateway-api",
                            "version" to apiVersion.get(),
                            "classes" to classes,
                        ),
                    ),
                ),
            )
            val versionOutput = versionFile.get().asFile
            versionOutput.parentFile.mkdirs()
            versionOutput.writeText("version=${apiVersion.get()}\n")
        }
    }

    private fun kindOf(type: Class<*>): String = when {
        type.isAnnotation -> "annotation"
        type.isEnum -> "enum"
        type.isRecord -> "record"
        type.isInterface -> "interface"
        else -> "class"
    }

    private fun isVisible(modifiers: Int): Boolean = Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)

    private fun constructorDescriptor(parameters: Array<Class<*>>): String =
        "(" + parameters.joinToString("") { it.descriptorString() } + ")V"

    private fun methodDescriptor(method: Method): String =
        "(" + method.parameterTypes.joinToString("") { it.descriptorString() } + ")" +
            method.returnType.descriptorString()
}

val generateGatewayApiAbi =
    tasks.register<GenerateGatewayApiAbi>("generateGatewayApiAbi") {
        group = "build"
        description = "Writes the public ABI manifest embedded in the gateway API jar"
        classesDirectory.set(layout.buildDirectory.dir("classes/kotlin/main"))
        outputFile.set(layout.buildDirectory.file("generated/abi/gateway-api.abi.json"))
        versionFile.set(layout.buildDirectory.file("generated/abi/gateway-api.properties"))
        apiVersion.set(project.version.toString())
        dependsOn(tasks.named("compileKotlin"))
    }

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateGatewayApiAbi)
    from(generateGatewayApiAbi.flatMap { it.outputFile }) {
        into("META-INF")
    }
    from(generateGatewayApiAbi.flatMap { it.versionFile }) {
        into("META-INF")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "api-management-gateway-api"
            pom {
                name.set("api-management-gateway-api")
                description.set("API management gateway plugin API")
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
