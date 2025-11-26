package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import org.openapitools.model.Bruker
import org.openapitools.model.TidspunktFraKilde

/**
 * Inneholder metadata om en endring i arbeidssøkerregisteret
 * @param tidspunkt Tidspunkt for endringen.
 * @param utfoertAv 
 * @param kilde Navn på systemet som utførte endringen eller ble benyttet til å utføre endringen
 * @param aarsak Aarasek til endringen. Feks \"Flyttet ut av landet\" eller lignende
 * @param tidspunktFraKilde 
 */
data class Metadata(

    @get:JsonProperty("tidspunkt", required = true) val tidspunkt: java.time.OffsetDateTime,

    @get:JsonProperty("utfoertAv", required = true) val utfoertAv: Bruker,

    @get:JsonProperty("kilde", required = true) val kilde: kotlin.String,

    @get:JsonProperty("aarsak", required = true) val aarsak: kotlin.String,

    @get:JsonProperty("tidspunktFraKilde") val tidspunktFraKilde: TidspunktFraKilde? = null
) {

}

