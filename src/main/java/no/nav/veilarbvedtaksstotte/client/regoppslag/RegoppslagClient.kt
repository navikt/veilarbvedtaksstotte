package no.nav.veilarbvedtaksstotte.client.regoppslag

import no.nav.common.health.HealthCheck

interface RegoppslagClient : HealthCheck {
    fun hentPostadresse(dto: RegoppslagRequestDTO): RegoppslagResponseDTO
}

data class RegoppslagRequestDTO(val ident: String, val tema: String)
data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse
) {
    data class Adresse(
        val type: AdresseType,
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String
    )

    enum class AdresseType {
        NORSKPOSTADRESSE, UTENLANDSKPOSTADRESSE
    }
}
