package no.nav.veilarbvedtaksstotte.domain

sealed interface DistribusjonBestillingId {
    val id: String

    object Feilet : DistribusjonBestillingId {
        override val id = "FEILET"
    }

    object Mangler : DistribusjonBestillingId {
        override val id = "-"
    }

    data class Uuid(override val id: String) : DistribusjonBestillingId
}
