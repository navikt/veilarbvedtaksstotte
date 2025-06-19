package no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto

data class BestemDistribusjonskanalResponseDTO (val distribusjonskanal: Distribusjonskanal, val regel: String,
                                                val regelBegrunnelse: String) {
    enum class Distribusjonskanal {
        PRINT,
        SDP,
        DITT_NAV,
        LOKAL_PRINT,
        INGEN_DISTRIBUSJON,
        TRYGDERETTEN,
        DPVT
    }

    val brukerKanIkkeVarsles: Boolean
        get() = distribusjonskanal == Distribusjonskanal.PRINT &&
                regel == "Bruker skal varsles, men finner hverken mobiltelefonnummer eller e-postadresse"
}