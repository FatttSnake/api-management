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

    testImplementation(libs.kotlin.test.junit5)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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
