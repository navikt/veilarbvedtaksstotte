package no.nav.veilarbvedtaksstotte.klagebehandling.domene

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import java.time.LocalDate
import java.util.UUID

data class Klagebehandling(
    val klageInitiellData: KlageInitiellData,
    val klageFormkravData: KlageFormkravData? = null,
    val klageResultatData: KlageResultatData? = null,
    val klageStatus: Status
)
typealias KlagebehandlingId = UUID

data class KlageInitiellData(
    val vedtakId: Long,
    val veilederIdent: String,
    val personIdenter: PersonIdenter,
    val klageDato: LocalDate,
    val klageJournalpostid: String,
) {
    data class PersonIdenter(
        val fnr: Fnr,
        val aktorId: AktorId? = null
    )
}

data class KlageFormkravData(
    val formkravSignert: FormkravSvar?,
    val formkravPart: FormkravSvar?,
    val formkravKonkret: FormkravSvar?,
    val formkravKlagefristOpprettholdt: FormkravSvar?,
    val formkravKlagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,
)

data class KlageResultatData(
    val resultat: Resultat,
    val resultatBegrunnelse: String?
)

data class KlageAvvisningData(
    val avvisningsbrevJournalpostId: String
)

enum class FormkravOppfylt {
    OPPFYLT, IKKE_OPPFYLT, IKKE_SATT
}

enum class Resultat {
    AVVIST, IKKE_SATT
}

enum class Status {
    UTKAST,
    AVVIST,
    FERDIGSTILT,
    SENDT_TIL_KABAL
}
