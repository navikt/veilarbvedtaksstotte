package no.nav.veilarbvedtaksstotte.klagebehandling.domene

import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.FormkravSvar
import java.time.LocalDate

data class KlageBehandling(
    val vedtakId: Long,
    val veilederIdent: String,
    val norskIdent: String,
    val klageDato: LocalDate?,
    val klageJournalpostid: String?,
    val formkravSignert : FormkravSvar,
    val formkravPart: FormkravSvar,
    val formkravKonkret: FormkravSvar,
    val formkravKlagefristOpprettholdt: FormkravSvar,
    val formkravKlagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravOppfylt: FormkravOppfylt,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,

    val resultat: Resultat,
    val resultatBegrunnelse: String?,
)
