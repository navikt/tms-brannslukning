package no.nav.tms.brannslukning.gui

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.tms.brannslukning.alert.AlertRepository
import no.nav.tms.token.support.azure.validation.AzurePrincipal
import no.nav.tms.token.support.azure.validation.azure

fun Application.gui(
    alertRepository: AlertRepository,
    authInstaller: Application.() -> Unit = {
        authentication {
            azure {
                setAsDefault = true
            }
        }
    }
) {

    val log = KotlinLogging.logger { }
    val secureLog = KotlinLogging.logger("secureLog")

    authInstaller()

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
        }
        exception<Throwable> { call, cause ->
            when (cause) {
                is BadFileContent -> {
                    log.error { "Mottok feil i fil" }
                    secureLog.error(cause) { "Mottok feil i fil" }
                    call.respondHtmlContent("Feil i identfil", true, HttpStatusCode.BadRequest) {
                        p {
                            +cause.message
                        }
                        a {
                            href = "/"
                            +"Tilbake"
                        }
                    }
                }


                is BadInputException -> {
                    log.error { "Mottok feil i input" }
                    secureLog.error(cause) { "Mottok feil i input" }
                    call.respondHtmlContent("Feil i tekster", true, HttpStatusCode.BadRequest) {
                        p {
                            +cause.message
                            cause.explanation.forEach { explanation ->
                                +explanation
                            }
                        }
                        a {
                            href = "/"
                            +"Tilbake"
                        }
                    }
                }

                is HendelseNotFoundException -> {
                    log.error { "Fant ikke hendelse" }
                    secureLog.error(cause) { "Fant ikke hendelse" }
                    call.respondHtmlContent("Hendelse ikke funnet", true, HttpStatusCode.NotFound) {
                        p {
                            +"Hendelsen du leter etter finnes ikke"
                        }
                        a {
                            href = "/"
                            +"Tilbake"
                        }
                    }
                }

                else -> {
                    log.error { "Ukjent feil" }
                    secureLog.error(cause) { "Ukjent feil" }
                    call.respondHtmlContent("Feil", true, HttpStatusCode.InternalServerError) {
                        p { +"Oops..Nå ble det noe feil" }
                        p { +"${cause.message}" }
                        img {
                            id = "500-katt"
                            src = "/static/500-katt.svg"
                            alt = "500-cat loves you!"
                            title = "500-cat loves you!"
                        }
                    }
                }
            }
        }
    }

    routing {
        meta()
        authenticate {
            startPage(alertRepository)
            opprettBeredskapvarsel(alertRepository)
            detaljerBeredskapvarsel(alertRepository)
        }
        staticResources("/static", "static") {
            preCompressed(CompressedFileType.GZIP)
        }
    }
}

fun Routing.meta() {
    get("isalive") {
        call.respond(HttpStatusCode.OK)
    }
    get("isready") {
        call.respond(HttpStatusCode.OK)
    }
}


val ApplicationCall.user
    get() = principal<AzurePrincipal>()?.let {
        User(
            oid = it.decodedJWT.getClaim("oid").toString(),
            username = it.decodedJWT.getClaim("preferred_username").toString().removeSurrounding("\"")
        )
    } ?: throw IllegalStateException("Må være innlogget")

