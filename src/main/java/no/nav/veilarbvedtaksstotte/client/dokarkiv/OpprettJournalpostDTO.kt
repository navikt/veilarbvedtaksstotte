package no.nav.veilarbvedtaksstotte.client.dokarkiv

import no.nav.common.types.identer.EnhetId

data class OpprettJournalpostDTO(
        val tittel: String,
        val journalpostType: JournalpostType,
        val tema: String,
        val journalfoerendeEnhet: EnhetId,
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
            val navn: String
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
