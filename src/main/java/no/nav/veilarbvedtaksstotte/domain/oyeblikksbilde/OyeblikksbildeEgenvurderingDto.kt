package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde

data class OyeblikksbildeEgenvurderingDto(
    val sistOppdatert: String? = null,
    val svar: List<Svar>? = null,
) {
    data class Svar(
        val spm: String,
        val svar: String? = null,
        val oppfolging: String? = null,
        val dialogId: String? = null,
    )
}
