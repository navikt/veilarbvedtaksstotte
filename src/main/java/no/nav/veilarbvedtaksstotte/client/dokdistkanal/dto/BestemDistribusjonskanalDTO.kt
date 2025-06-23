package no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto

data class BestemDistribusjonskanalDTO (
    val brukerId: String,
    val mottakerId: String = brukerId, // i vårt tilfelle er bruker og mottaker samme person
    val tema: String = "OPP",
    val erArkivert: Boolean = true, // responsen defaulter til PRINT uten denne
)
