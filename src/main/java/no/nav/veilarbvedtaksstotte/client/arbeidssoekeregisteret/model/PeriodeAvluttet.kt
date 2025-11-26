package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.Bruker
import org.openapitools.model.TidspunktFraKilde

/**
 * Inneholder data om en startet periode
 * @param type 
 * @param tidspunkt Tidspunkt for endringen
 * @param utfoertAv 
 * @param kilde Navn på systemet som utførte endringen eller ble benyttet til å utføre endringen
 * @param aarsak Aarasek til endringen. Feks \"Flyttet ut av landet\" eller lignende
 * @param tidspunktFraKilde 
 */
data class PeriodeAvluttet(

    @get:JsonProperty("type", required = true) val type: PeriodeAvluttet.Type,

    @get:JsonProperty("tidspunkt", required = true) val tidspunkt: java.time.OffsetDateTime,

    @get:JsonProperty("utfoertAv", required = true) val utfoertAv: Bruker,

    @get:JsonProperty("kilde", required = true) val kilde: kotlin.String,

    @get:JsonProperty("aarsak", required = true) val aarsak: kotlin.String,

    @get:JsonProperty("tidspunktFraKilde") val tidspunktFraKilde: TidspunktFraKilde? = null
) {

    /**
    * 
    * Values: PERIODE_AVSLUTTET_V1
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        PERIODE_AVSLUTTET_V1("PERIODE_AVSLUTTET_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'PeriodeAvluttet'")
            }
        }
    }

}

