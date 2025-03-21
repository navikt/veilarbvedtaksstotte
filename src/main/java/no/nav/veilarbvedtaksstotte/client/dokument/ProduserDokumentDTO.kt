package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr

data class ProduserDokumentDTO(
    val brukerFnr: Fnr,
    val navn: String,
    val malType: MalType,
    val enhetId: EnhetId,
    val veilederIdent: String,
    val begrunnelse: String?,
    val opplysninger: List<String>,
    val utkast: Boolean,
)
