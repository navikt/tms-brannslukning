import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)

    id(TmsJarBundling.plugin)

    // Apply the application plugin to add support for building a CLI application.
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
    implementation(Flyway.core)
    implementation(Flyway.postgres)
    implementation(Hikari.cp)
    implementation(Caffeine.caffeine)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(JacksonDatatype.moduleKotlin)
    implementation(Kafka.clients)
    implementation(KotliQuery.kotliquery)
    implementation(KotlinLogging.logging)
    implementation(Ktor303Server.core)
    implementation(Ktor303Server.netty)
    implementation(Ktor303Server.htmlDsl)
    implementation(Ktor303Server.statusPages)
    implementation(Ktor303Server.auth)
    implementation(Ktor303Server.authJwt)
    implementation(Ktor303Client.contentNegotiation)
    implementation(Ktor303Client.apache)
    implementation(Ktor303Serialization.jackson)
    implementation(Logstash.logbackEncoder)
    implementation(Postgresql.postgresql)
    implementation(TmsCommonLib.utils)
    implementation(TmsKtorTokenSupport.azureValidation)
    implementation(TmsVarselBuilder.kotlinBuilder)
    implementation(TmsKtorTokenSupport.azureValidation)
    implementation(TmsKafkaTools.kafkaApplication)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JunitJupiter.api)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(TmsKtorTokenSupport.azureValidationMock)
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
