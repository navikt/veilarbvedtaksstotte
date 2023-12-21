package no.nav.veilarbvedtaksstotte.client.aiaBackend.dto

import java.time.LocalDateTime

data class EndringIRegistreringsdataResponse(
    val registreringsId: Number? = null,
    val besvarelse: Besvarelse? = null,
    val endretAv: String? = null,
    val endretTidspunkt: LocalDateTime? = null,
    val registreringsTidspunkt: LocalDateTime? = null,
    val opprettetAv: String? = null,
    val erBesvarelsenEndret: Boolean? = null
)
