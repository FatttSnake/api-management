pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "api-management"

include(":gateway-api", ":plugin-runtime", ":plugin-signing", ":plugin-gradle-plugin")
