package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import no.nav.common.types.identer.Fnr
import java.time.LocalDate

data class OppdaterFormkravRequest(
    val vedtakId: Long,
    val signert: FormkravSvar?,
    val part: FormkravSvar?,
    val konkret: FormkravSvar?,
    val klagefristOpprettholdt: FormkravSvar?,
    val klagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,
)

data class AvvisKlageRequest(
    val vedtakId: Long,
    val signert: FormkravSvar,
    val part: FormkravSvar,
    val konkret: FormkravSvar,
    val klagefristOpprettholdt: FormkravSvar,
    val klagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String
)

data class FullførKlageAvvisningRequest(
    val vedtakId: Long,
    val avvisningsbrevJournalpostId: String
)

data class OpprettKlageRequest(
    val vedtakId: Long,
    val fnr: Fnr,
    val veilederIdent: String,
    val klagedato: LocalDate,
    val klageJournalpostid: String,
)

data class HentKlageRequest(
    val vedtakId: Long
)

enum class FormkravSvar {
    JA, NEI
}


enum class FormkravKlagefristUnntakSvar {
    JA_KLAGER_KAN_IKKE_LASTES, JA_SAERLIGE_GRUNNER, NEI
}
