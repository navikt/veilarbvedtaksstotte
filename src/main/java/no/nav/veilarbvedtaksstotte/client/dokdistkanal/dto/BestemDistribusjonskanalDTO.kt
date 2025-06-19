package no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto

data class BestemDistribusjonskanalDTO (
    val brukerId: String,
    val mottakerId: String = brukerId,
    val tema: String = "OPP",
    val erArkiveret: Boolean = true,
)