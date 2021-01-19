package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr

data class ProduserDokumentV2DTO(
    val brukerFnr: Fnr,
    val malType: MalType,
    val enhetId: EnhetId,
    val begrunnelse: String?,
    val opplysninger: List<String>,
    val utkast: Boolean
)
