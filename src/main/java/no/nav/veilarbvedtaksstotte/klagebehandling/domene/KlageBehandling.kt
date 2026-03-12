package no.nav.veilarbvedtaksstotte.klagebehandling.domene

import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import java.time.LocalDate

data class KlageBehandling(
    val generellData: GenerellData,
    val formkravData: FormkravData? = null,
    val resultatData: ResultatData? = null
)

data class GenerellData(
    val vedtakId: Long,
    val veilederIdent: String,
    val norskIdent: String,
    val klageDato: LocalDate,
    val klageJournalpostid: String,
)

data class FormkravData(
    val formkravSignert: FormkravSvar?,
    val formkravPart: FormkravSvar?,
    val formkravKonkret: FormkravSvar?,
    val formkravKlagefristOpprettholdt: FormkravSvar?,
    val formkravKlagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,
    val formkravOppfylt: FormkravOppfylt? = null
)

data class ResultatData(
    val resultat: Resultat,
    val resultatBegrunnelse: String?,
    val status: Status
)

enum class FormkravOppfylt {
    OPPFYLT, IKKE_OPPFYLT, IKKE_SATT
}

enum class Resultat {
    AVVIST, IKKE_SATT
}

enum class Status {
    UTKAST, SENDT_TIL_KABAL, FERDIGSTILT // todo finne ut hva hvilke statuser vi trenger
}