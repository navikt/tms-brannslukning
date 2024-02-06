package no.nav.tms.brannslukning.alert

import no.nav.tms.brannslukning.setup.PeriodicJob
import no.nav.tms.brannslukning.setup.PodLeaderElection
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.action.Varseltype.Beskjed
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import java.util.UUID

class VarselPusher(
    private val alertRepository: AlertRepository,
    private val leaderElection: PodLeaderElection,
    private val kafkaProducer: Producer<String, String>,
    private val varselTopic: String,
    private val batchSize: Int = 500,
    private val produsentOverride: Produsent? = null,
    interval: Duration = Duration.ofSeconds(1)
) : PeriodicJob(interval = interval) {

    override val job = initializeJob {
        if (leaderElection.isLeader()) {
            alertRepository.nextInVarselQueue(batchSize)
                .forEach { request ->
                    val varselId = sendBeskjed(request)
                    alertRepository.markAsSent(
                        referenceId = request.referenceId,
                        ident = request.ident,
                        varselId = varselId
                    )
                }
        }
    }

    private fun sendBeskjed(varselRequest: VarselRequest): String {
        val beskjedId = UUID.randomUUID().toString()

        VarselActionBuilder.opprett {
            type = Beskjed
            varselId = beskjedId
            ident = varselRequest.ident
            link = varselRequest.beskjed.link
            tekst = Tekst(
                spraakkode = varselRequest.beskjed.spraakkode,
                tekst = varselRequest.beskjed.tekst,
                default = true
            )
            eksternVarsling = EksternVarslingBestilling(
                prefererteKanaler = listOf(EksternKanal.SMS),
                smsVarslingstekst = varselRequest.eksternTekst.tekst,
                epostVarslingstekst = varselRequest.eksternTekst.tekst,
                epostVarslingstittel = varselRequest.eksternTekst.tittel
            )
            sensitivitet = Sensitivitet.Substantial
            produsentOverride?.let {
                produsent = it
            }
            metadata["beredskap_tittel"] = varselRequest.tittel
            metadata["beredskap_ref"] = varselRequest.referenceId

        }.let { beskjed ->
            kafkaProducer.send(ProducerRecord(varselTopic, beskjedId, beskjed))
        }

        return beskjedId
    }
}
