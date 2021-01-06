package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr

data class SendDokumentDTO(
    val brukerFnr: Fnr,
    val malType: MalType,
    val enhetId: EnhetId,
    val begrunnelse: String?,
    val opplysninger: List<String>
)
