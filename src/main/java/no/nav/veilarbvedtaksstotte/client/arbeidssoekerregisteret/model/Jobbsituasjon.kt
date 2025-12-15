package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Inneholder et sett med beskrivelser av jobbsituasjonen. Det er mulig å ha flere beskrivelser av jobbsituasjonen, feks kan personen være permittert og samtidig ha en deltidsjobb.
 * @param beskrivelser
 */
data class Jobbsituasjon(

    @get:JsonProperty("beskrivelser", required = true) val beskrivelser: List<BeskrivelseMedDetaljer>
)

