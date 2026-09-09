import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

group = "top.fatweb"
version = "1.0.0-SNAPSHOT"
description = "api-management plugin authoring Gradle plugin"

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
    // Reuses PluginSigner / PluginDescriptor from the SDK so plugins are signed
    // in-process (no subprocess) with exactly the same code as the gateway's verifier.
    implementation(project(":plugin-sdk"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

gradlePlugin {
    plugins {
        create("apiPlugin") {
            id = "top.fatweb.api-plugin"
            implementationClass = "top.fatweb.apimanagement.gradle.ApiPlugin"
            displayName = "API Management Plugin"
            description = "Scaffolds, signs and packages plugins for the API Management gateway"
        }
    }
}

publishing {
    publications {
        // Must be named `pluginMaven` so `java-gradle-plugin` attaches the
        // plugin-marker artifact to it (that is how `id("top.fatweb.api-plugin")`
        // resolves from a maven repository / mavenLocal).
        create<MavenPublication>("pluginMaven") {
            // `java-gradle-plugin` attaches the `java` component to a publication
            // named `pluginMaven` automatically — do not call from(components["java"]) here.
            artifactId = "api-management-plugin-gradle-plugin"
            pom {
                name.set("api-management-plugin-gradle-plugin")
                description.set("Gradle plugin for building and signing API Management gateway plugins")
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
