import org.gradle.api.publish.PublishingExtension

// Publishing target for api-management's Maven publications.
//
// No credentials live here. The Nexus username/password are read from:
//   1. nexusUsername / nexusPassword in ~/.gradle/gradle.properties
//   2. NEXUS_USERNAME / NEXUS_PASSWORD environment variables
//
// A machine-local gradle/nexus-publish.local.gradle.kts (gitignored) takes
// precedence, so a maintainer can point at a different Nexus without touching
// this file.

val nexusRepositories = "https://nexus.fatweb.top/repository"

subprojects {
    plugins.withId("maven-publish") {
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "nexus"
                        url =
                            uri(
                                if (project.version.toString().endsWith("-SNAPSHOT")) {
                                    "$nexusRepositories/maven-snapshots/"
                                } else {
                                    "$nexusRepositories/maven-releases/"
                                },
                            )
                        credentials {
                            username =
                                providers
                                    .gradleProperty("nexusUsername")
                                    .orElse(providers.environmentVariable("NEXUS_USERNAME"))
                                    .orNull
                            password =
                                providers
                                    .gradleProperty("nexusPassword")
                                    .orElse(providers.environmentVariable("NEXUS_PASSWORD"))
                                    .orNull
                        }
                    }
                }
            }
        }
    }
}
