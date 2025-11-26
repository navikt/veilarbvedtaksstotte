package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import org.openapitools.model.BeskrivelseMedDetaljer

/**
 * Inneholder et sett med beskrivelser av jobbsituasjonen. Det er mulig å ha flere beskrivelser av jobbsituasjonen, feks kan personen være permittert og samtidig ha en deltidsjobb. 
 * @param beskrivelser 
 */
data class Jobbsituasjon(

    @get:JsonProperty("beskrivelser", required = true) val beskrivelser: kotlin.collections.List<BeskrivelseMedDetaljer>
) {

}

