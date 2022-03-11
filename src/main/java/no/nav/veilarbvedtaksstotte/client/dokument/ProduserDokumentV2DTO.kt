package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr

data class ProduserDokumentV2DTO(
    val brukerFnr: Fnr,
    val navn: String,
    val malType: MalType,
    val enhetId: EnhetId,
    val begrunnelse: String?,
    val opplysninger: List<String>,
    val utkast: Boolean,
    val adresse: AdresseDTO
) {
    data class AdresseDTO(
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val land: String?
    )
}
