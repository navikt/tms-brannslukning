package no.nav.tms.brannslukning.common.gui

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

private val log = KotlinLogging.logger { }

fun Routing.opprettHendelse() {

    route("opprett") {
        get {
            val hendelse = call.hendelseOrNull()
            call.respondHtmlContent("Opprett hendelse – tekster") {
                body {
                    h1 { +"Legg inn tekster for varsling" }
                    form {
                        action = "/opprett"
                        method = FormMethod.post
                        fieldSet {
                            legend {
                                +"Beskjed på min side"
                            }
                            label {
                                htmlFor = "beskjed-input"
                                +"Tekst"
                            }
                            textArea(classes = "text-input") {
                                id = "beskjed-input"
                                name = "beskjed-text"
                                required = true
                                maxLength = "150"
                                minLength = "50"
                                hendelse?.let {
                                    text(it.varseltekst)
                                }
                            }
                            label {
                                htmlFor = "url-input"
                                +"Link til mer informasjon"
                            }
                            input {
                                id = "url-input"
                                name = "url-text"
                                type = InputType.url
                                required = true
                                minLength = "15"
                                hendelse?.let {
                                    value = hendelse.url
                                }
                            }
                        }
                        fieldSet {
                            id = "ekstern-tekst-fieldset"
                            legend {
                                +"Varsel på sms/epost"
                            }
                            label {
                                htmlFor = "ekstern-tekst-input"
                                +"Tekst"
                            }
                            textArea(classes = "text-input") {
                                id = "ekstern-tekst-input"
                                name = "ekstern-text"
                                required = true
                                maxLength = "150"
                                minLength = "50"
                                hendelse?.let {
                                    text(it.eksternTekst)
                                }
                            }
                        }
                        cancelAndGoBackButtons()
                        button {
                            type = ButtonType.submit
                            text("Neste")
                        }
                    }
                }
            }
        }
        post {
            val hendelse = call.hendelseOrNull()
            val params = call.receiveParameters()
            val beskjedTekst =
                params["beskjed-text"] ?: throw IllegalArgumentException("Tekst for beskjed må være satt")
            val url =
                params["url-text"] ?: throw IllegalArgumentException("Url for beskjed må være satt")
            val eksternTekst =
                params["ekstern-text"] ?: throw IllegalArgumentException("Tekst for sms/epost må være satt")
            val tmpHendelse =
                hendelse?.withUpdatedText(beskjedTekst = beskjedTekst, url = url, eksternTekst = eksternTekst)
                    ?: TmpHendelse(
                        varseltekst = beskjedTekst,
                        eksternTekst = eksternTekst,
                        initatedBy = call.user,
                        url = url
                    )
            HendelseChache.putHendelse(tmpHendelse)
            call.respondSeeOther("opprett/personer?hendelse=${tmpHendelse.id}")
        }

        route("personer") {
            get {
                call.respondUploadFileForm(call.hendelse())
            }
            post {
                val hendelse = call.hendelse()
                val multipartData = call.receiveMultipart()
                var fileDescription = ""
                var fileName = ""
                var content = byteArrayOf()

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            fileDescription = part.value
                        }

                        is PartData.FileItem -> {
                            fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            content += fileBytes
                            log.info { content }
                        }

                        else -> {
                            throw IllegalArgumentException("Ukjent innholdt i opplastet fil")
                        }
                    }
                    part.dispose()
                }

                log.info { "$fileName opplastet\n$fileDescription" }
                val idents = content.parseAndVerify() ?: hendelse.affectedUsers
                HendelseChache.putHendelse(hendelse.withAffectedUsers(idents))
                call.respondSeeOther("/opprett/confirm?hendelse=${hendelse.id}")
            }
        }

        route("confirm") {
            get {
                val hendelse = call.hendelse()
                call.respondHtmlContent("Opprett hendelse – bekreft") {
                    body {
                        h1 { +"Bekreft" }
                        hendelseDl(hendelse)
                        form {
                            action = "/send/confirm?hendelse=${hendelse.id}"
                            method = FormMethod.post
                            button {
                                type = ButtonType.submit
                                text("Opprett hendelse")
                            }
                            cancelAndGoBackButtons("/opprett/personer?hendelse=${hendelse.id}")
                        }
                    }
                }

            }
        }

    }

    route("send") {

        post("confirm") {
            val hendelse = call.hendelse()
            //TODO database og kafka og fest
            log.info { "TODO: Lagre i database og send varsel på kafka" }
            HendelseChache.invalidateHendelse(hendelse.id)

            call.respondHtmlContent("Hendelse opprettet") {
                body {
                    h1 { +"Hendelse opprettet" }
                    hendelseDl(hendelse)
                    a {
                        href = "/"
                        +"Tilbake til forsiden"
                    }

                }

            }

        }
    }
}

private fun ByteArray.parseAndVerify(): List<String>? =
    String(this).lines()
        .filter { it.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
        ?.apply {
            if (this.any { identStr -> identStr.toDoubleOrNull() == null }) {
                throw BadFileContent("Liste av identer inneholder ugyldige tegn")
            }
            if (this.any { identStr -> identStr.length != 11 }) {
                throw BadFileContent("Liste av identer inneholder identer med feil antall siffer")
            }
        }

class BadFileContent(override val message: String) : IllegalArgumentException()