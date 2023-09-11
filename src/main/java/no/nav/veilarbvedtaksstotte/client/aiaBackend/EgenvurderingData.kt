package no.nav.veilarbvedtaksstotte.client.aiaBackend

data class EgenvurderingData(
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
