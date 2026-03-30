package no.nav.tms.brannslukning

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.tms.brannslukning.alert.AlertRepository
import no.nav.tms.brannslukning.alert.EksterntVarselStatusSubscriber
import no.nav.tms.brannslukning.alert.VarselInaktivertSubscriber
import no.nav.tms.brannslukning.alert.VarselPusher
import no.nav.tms.brannslukning.gui.gui
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.kafka.application.KafkaApplication
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.flywaydb.core.Flyway
import java.util.*

fun main() {
    if (Environment.isDevMode()) {
        startDevServer()
    } else {
        startKafkaApplication()
    }
}

private fun startDevServer() {
    val database = Postgres.connectToJdbcUrl(Environment.jdbcUrl())

    embeddedServer(
        Netty,
        configure = {
            connector {
                port = 8080
            }
        },
        module = {
            gui(AlertRepository(database))
            monitor.subscribe(ApplicationStarted) {
                Flyway.configure()
                    .dataSource(database.dataSource)
                    .load()
                    .migrate()
            }
        }
    ).start(wait = true)
}

private fun startKafkaApplication() {
    val database = Postgres.connectToJdbcUrl(Environment.jdbcUrl())

    val alertRepository = AlertRepository(database)
    val kafkaProducer = initializeKafkaProducer()
    val varselPusher = VarselPusher(
        alertRepository = alertRepository,
        kafkaProducer = kafkaProducer,
        varselTopic = Environment.varselTopic
    )

    KafkaApplication.build {
        ktorModule {
            gui(alertRepository)
        }

        kafkaConfig {
            groupId = Environment.groupId
            readTopic(Environment.readVarselTopic)
        }
        subscribers(
            VarselInaktivertSubscriber(alertRepository),
            EksterntVarselStatusSubscriber(alertRepository)
        )

        onStartup {
            Flyway.configure()
                .dataSource(database.dataSource)
                .load()
                .migrate()
        }

        onReady {
            varselPusher.start()
        }

        onShutdown {
            runBlocking {
                varselPusher.stop()
                kafkaProducer.flush()
                kafkaProducer.close()
            }
        }
    }.start()
}

private fun initializeKafkaProducer(environment: KafkaEnvironment = KafkaEnvironment()) = KafkaProducer<String, String>(
    Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.kafkaBrokers)
        put(
            ProducerConfig.CLIENT_ID_CONFIG,
            "tms-brannslukning"
        )
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
        put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, environment.kafkaTruststorePath)
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, environment.kafkaCredstorePassword)
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, environment.kafkaKeystorePath)
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, environment.kafkaCredstorePassword)
        put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, environment.kafkaCredstorePassword)
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
    }
)
