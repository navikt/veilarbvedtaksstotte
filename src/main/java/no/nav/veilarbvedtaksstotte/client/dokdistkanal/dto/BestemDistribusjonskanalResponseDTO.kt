package no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto

data class BestemDistribusjonskanalResponseDTO (val distribusjonskanal: String, val regel: String,
                                                val regelBegrunnelse: String) {
    enum class Distribusjonskanal {
        PRINT
    }

    fun erDistribusjonskanalPrint(kanal: String): Boolean = when (kanal) {
        Distribusjonskanal.PRINT.toString() -> true
        else -> false
    }

    enum class Regel {
        BRUKER_SDP_MANGLER_VARSELINFO
    }

    val brukerKanIkkeVarsles: Boolean
        get() = erDistribusjonskanalPrint(distribusjonskanal) &&
                regel == Regel.BRUKER_SDP_MANGLER_VARSELINFO.toString()
}
