package no.nav.tms.brannslukning.alert.gui

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import no.nav.tms.brannslukning.alert.AlertRepository
import no.nav.tms.brannslukning.alert.setup.database.LocalPostgresDatabase
import no.nav.tms.brannslukning.gui.BeredskapvarselCache
import no.nav.tms.brannslukning.gui.gui
import no.nav.tms.token.support.azure.validation.mock.azureMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class OpprettBeredskapsvarselTest {

    private val database = LocalPostgresDatabase.cleanDb()

    @AfterEach
    fun cleandb() {
        database.clearTables()
        BeredskapvarselCache.clearCache()
    }

    @Test
    fun `opprettet tittel og beskrivelse av varsel og lagrer i cache`() = apiTest {
        val redirect = client.submitForm(
            url = "/varsel/bakgrunn",
            formParameters = parameters {
                append("description", "Beskrivelse av hendelse")
                append("title", "Tittel på hendelse")
            }
        )

        redirect.status shouldBe HttpStatusCode.SeeOther

        val location = redirect.headers[HttpHeaders.Location].shouldNotBeNull()
        val response = client.get(location)

        response.status shouldBe HttpStatusCode.OK

        val hendelseId = location.hendelseIdFromPath().shouldNotBeNull()

        BeredskapvarselCache.getHendelse(hendelseId)
            .shouldNotBeNull()
            .let {
                it.title shouldBe "Tittel på hendelse"
                it.description shouldBe "Beskrivelse av hendelse"
            }
    }

    @KtorDsl
    fun apiTest(testBlock: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            gui(
                alertRepository = AlertRepository(database),
                authInstaller = {
                    authentication {
                        azureMock {
                            setAsDefault = true
                            alwaysAuthenticated = true
                        }
                    }
                }
            )
        }

        testBlock()
    }

    private fun String.hendelseIdFromPath() = "/varsel/([a-f0-9-]+)(/.*)?".toRegex()
        .find(this)
        ?.destructured
        ?.let { (hendelseId, _) -> hendelseId }
}
