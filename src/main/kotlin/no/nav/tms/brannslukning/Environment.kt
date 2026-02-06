package no.nav.tms.brannslukning

import no.nav.tms.common.util.config.BooleanEnvVar.getEnvVarAsBoolean
import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

object Environment {
    val varselTopic = "min-side.aapen-brukervarsel-v1"
    val readVarselTopic = "min-side.aapen-varsel-hendelse-v1"
    val groupId = "brannslukning-01"

    fun isDevMode() = getEnvVarAsBoolean("DEV_MODE", false)

    fun jdbcUrl(): String {
        val host: String = getEnvVar("DB_HOST")
        val name: String = getEnvVar("DB_DATABASE")
        val user: String = getEnvVar("DB_USERNAME")
        val password: String = getEnvVar("DB_PASSWORD")

        return "jdbc:postgresql://${host}/$name?user=$user&password=$password"
    }
}

data class KafkaEnvironment(
    val kafkaBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val kafkaSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val kafkaTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    val kafkaKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
    val kafkaCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
    val kafkaSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
    val kafkaSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
)
