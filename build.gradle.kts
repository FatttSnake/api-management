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

    implementation(libs.bcprov.jdk18on)

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
