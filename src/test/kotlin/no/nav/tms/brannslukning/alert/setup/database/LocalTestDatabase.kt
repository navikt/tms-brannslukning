package no.nav.tms.brannslukning.alert.setup.database

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.common.postgres.PostgresDatabase
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

object LocalTestDatabase {

    private val container = PostgreSQLContainer("postgres:15.5").also {
        it.start()
    }

    private val database by lazy {
        Postgres.connectToContainer(container).also {
            migrate(it.dataSource)
        }
    }

    fun getInstance(): PostgresDatabase {
        return database
    }

    fun resetInstance() {
        database.update { queryOf("delete from aktiv_alert_regel") }
        database.update { queryOf("delete from alert_beskjed_queue") }
        database.update { queryOf("delete from alert_header") }
    }

    private fun migrate(dataSource: HikariDataSource) {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    fun getVarslerForAlert(referenceId: String): List<VarselData> =
        database.list {
            queryOf(
                """
                select * from alert_beskjed_queue
                where alert_ref = :refId 
            """.trimIndent(), mapOf("refId" to referenceId)
            ).map { row ->
                row.stringOrNull("varselId")?.let {
                    VarselData(
                        varselId = it,
                        ident = row.string("ident"),
                        lest = row.boolean("varsel_lest"),
                        eksternStatus = row.stringOrNull("status_ekstern")

                    )
                } ?: VarselData(
                    ident = row.string("ident")
                )
            }
        }
}

class VarselData(
    val varselId: String? = null,
    val lest: Boolean = false,
    val eksternStatus: String? = null,
    val ident: String
)
