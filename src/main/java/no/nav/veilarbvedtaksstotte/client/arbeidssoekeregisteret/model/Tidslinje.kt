package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import org.openapitools.model.Hendelse

/**
 * Tidslinje for en Arbeidssøkerperiode
 * @param periodeId UUID for perioden som tidslinjen gjelder
 * @param identitetsnummer Identitetsnummer for arbeidssøker, fødselsnummer eller d-nummer
 * @param startet Starttidspunkt for perioden
 * @param hendelser 
 * @param avsluttet Avslutningstidspunkt for perioden, ikke satt dersom perioden er pågående
 */
data class Tidslinje(

    @get:JsonProperty("periodeId", required = true) val periodeId: java.util.UUID,

    @get:JsonProperty("identitetsnummer", required = true) val identitetsnummer: kotlin.String,

    @get:JsonProperty("startet", required = true) val startet: java.time.OffsetDateTime,

    @get:JsonProperty("hendelser", required = true) val hendelser: kotlin.collections.List<Hendelse>,

    @get:JsonProperty("avsluttet") val avsluttet: java.time.OffsetDateTime? = null
) {

}

