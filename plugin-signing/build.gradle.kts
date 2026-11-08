plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    `java-library`
    `maven-publish`
}

version = providers.gradleProperty("pluginToolingVersion").get()
description = "api-management plugin signing"

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

    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "api-management-plugin-signing"
            pom {
                name.set("api-management-plugin-signing")
                description.set("API management plugin Ed25519 signing")
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
