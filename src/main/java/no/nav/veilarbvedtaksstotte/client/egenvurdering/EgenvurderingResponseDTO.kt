package no.nav.veilarbvedtaksstotte.client.egenvurdering

data class EgenvurderingResponseDTO(
    val dato: String? = null,
    val dialogId: String? = null,
    val oppfolging: String? = null,
    val tekster: Tekster
) {
    data class Tekster(
        val sporsmal: String,
        val svar: Map<String, String>
    )
}

