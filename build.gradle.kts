import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(managed.versions.kotlin)
    alias(managed.plugins.tms.jarBundling)
    application
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {

    implementation(managed.bundles.database)
    implementation(managed.tms.commonLib.postgres)
    implementation(managed.kotliQuery)
    implementation(managed.kafka.clients)
    implementation(managed.kotlinLogging)
    implementation(managed.bundles.ktorServer)
    implementation(managed.ktor.server.html)
    implementation(managed.bundles.ktorClient)
    implementation(managed.ktor.serialization.jackson)
    implementation(managed.bundles.jackson)
    implementation(managed.logback.encoder)
    implementation(managed.tms.commonLib.teamLogger)
    implementation(managed.tms.commonLib.utils)
    implementation(managed.tms.kafka.application)
    implementation(managed.tms.ktorTokenSupport.entraId.verification)

    implementation(libs.tms.varsel.builder)
    implementation(libs.caffeine)

    testImplementation(managed.bundles.junit)
    testImplementation(managed.mockk)
    testImplementation(managed.bundles.testContainers)
    testImplementation(managed.ktor.test.server)
    testImplementation(managed.bundles.kotest)
    testImplementation(managed.tms.ktorTokenSupport.entraId.verificationMock)
}

application {
    mainClass.set("no.nav.tms.brannslukning.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
