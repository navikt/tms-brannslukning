import kotlinx.html.*
import no.nav.tms.brannslukning.common.gui.columnTh

class VarselStatus(
    val antallFerdigstilteVarsler: Int,
    val antallLesteVarsler: Int,
    val eksterneVarslerStatus: EksterneVarslerStatus
) {
    val lestProsent = (antallLesteVarsler / antallFerdigstilteVarsler) * 100
}

class EksterneVarslerStatus(
    val antallBestilt: Int,
    val antallSendt: Int,
    val antallFeilet: Int
) {
    val utsendelseFerdig = antallBestilt == (antallSendt + antallFeilet)
}

enum class EksternStatus(val priority: Int) {
    bestilt(1), feilet(2), sendt(3);

    companion object {
        fun resolve(old: String?, new: String): String =
            when {
                old == null -> new
                valueOf(old).priority > valueOf(new).priority -> old
                else -> new
            }
    }
}

fun TR.statusHeaders() {
    columnTh(
        "Antall personer",
        "Eksterne varsler sendt",
        "Eksterne varsler feiler",
        "Beskjed åpnet"
    )
}

fun TR.statusColumns(
    status: VarselStatus
) {
    td { +"${status.antallFerdigstilteVarsler}" } //"Antall personer",
    td { +"${status.eksterneVarslerStatus.antallSendt}" }//"Eksterne varsler sendt"
    td { +"${status.eksterneVarslerStatus.antallFeilet}" }//"Eksterne varsler feiler"
    td { +"${status.lestProsent}" }//"Beskjed åpnet"
}