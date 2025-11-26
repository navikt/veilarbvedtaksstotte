package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.Bekreftelsesloesning

/**
 * 
 * @param type 
 * @param periodeId UUID for perioden som meldingen gjelder, hentes fra Periode topic eller oppslags api
 * @param bekreftelsesloesning 
 * @param fristBrutt Angir om grunnen til stopp meldingen er at brukeren har brutt fristen i det eksterne systemet. Dersom denne er satt til true og fristene som ble meldt inn i start meldingen er like eller lenger enn registerets egne frister vil arbeidssøkerperioden bli stoppet umidelbart. 
 */
data class PaaVegneAvStopp(

    @get:JsonProperty("type", required = true) val type: PaaVegneAvStopp.Type,

    @get:JsonProperty("periodeId", required = true) val periodeId: java.util.UUID,

    @get:JsonProperty("bekreftelsesloesning", required = true) val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("fristBrutt") val fristBrutt: kotlin.Boolean? = false
) {

    /**
    * 
    * Values: PAA_VEGNE_AV_STOPP_V1
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        PAA_VEGNE_AV_STOPP_V1("PAA_VEGNE_AV_STOPP_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'PaaVegneAvStopp'")
            }
        }
    }

}

