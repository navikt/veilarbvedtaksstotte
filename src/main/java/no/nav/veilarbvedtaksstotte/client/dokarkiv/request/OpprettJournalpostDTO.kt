package no.nav.veilarbvedtaksstotte.client.dokarkiv.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import no.nav.common.types.identer.EnhetId

data class OpprettJournalpostDTO(
    val tittel: String,
    val journalpostType: JournalpostType,
    val tema: String,
    val journalfoerendeEnhet: EnhetId,
    val eksternReferanseId: String, // For duplikatkontroll
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val sak: Sak,
    val dokumenter: List<Dokument>
) {

    enum class JournalpostType(val value: String) {
        INNGAAENDE("INNGAAENDE"),
        UTGAAENDE("UTGAAENDE"),
        NOTAT("NOTAT")
    }

    data class AvsenderMottaker(
        val id: String,
        val idType: IdType,
        // Det er ikke nødvendig å oppgi navn når idType=FNR. Tjenesten vil da utlede navnet fra PDL.
        @JsonInclude(Include.NON_NULL)
        val navn: String? = null
    ) {
        enum class IdType(val value: String) {
            FNR("FNR"),
            ORGNR("ORGNR"),
            HPRNR("HPRNR"),
            UTL_ORG("UTL_ORG")
        }
    }

    data class Bruker(
        val id: String,
        val idType: IdType
    ) {
        enum class IdType(val value: String) {
            FNR("FNR"),
            ORGNR("ORGNR"),
            AKTOERID("AKTOERID")
        }
    }

    data class Sak(
        val fagsakId: String,
        val fagsaksystem: String,
        val sakstype: Type
    ) {
        enum class Type(val value: String) {
            FAGSAK("FAGSAK"),
            GENERELL_SAK("GENERELL_SAK"),
            ARKIVSAK("ARKIVSAK")
        }
    }

    data class Dokument(
        val tittel: String,
        val brevkode: String,
        val dokumentvarianter: List<DokumentVariant>
    )

    data class DokumentVariant(
        val filtype: String,
        val fysiskDokument: ByteArray,
        val variantformat: String
    )
}
