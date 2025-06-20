package no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto

data class BestemDistribusjonskanalResponseDTO (val distribusjonskanal: Distribusjonskanal, val regel: String,
                                                val regelBegrunnelse: String) {
    enum class Distribusjonskanal {
        PRINT
    }

    enum class Regel {
        BRUKER_SDP_MANGLER_VARSELINFO
    }

    val brukerKanIkkeVarsles: Boolean
        get() = distribusjonskanal == Distribusjonskanal.PRINT &&
                regel == Regel.BRUKER_SDP_MANGLER_VARSELINFO.toString()
}
