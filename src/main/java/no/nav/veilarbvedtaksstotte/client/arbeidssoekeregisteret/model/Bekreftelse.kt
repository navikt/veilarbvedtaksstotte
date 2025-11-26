package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.BekreftelsStatus
import org.openapitools.model.Bekreftelsesloesning
import org.openapitools.model.Svar

/**
 * Bekreftelse melding
 * @param type 
 * @param id Unik id for meldingen. Duplikater sees på som nettverkshikke eller lignende og skal trygt kunne ignoreres
 * @param bekreftelsesloesning 
 * @param status 
 * @param svar 
 */
data class Bekreftelse(

    @get:JsonProperty("type", required = true) val type: Bekreftelse.Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("bekreftelsesloesning", required = true) val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("status", required = true) val status: BekreftelsStatus,

    @get:JsonProperty("svar", required = true) val svar: Svar
) {

    /**
    * 
    * Values: BEKREFTELSE_V1
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        BEKREFTELSE_V1("BEKREFTELSE_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Bekreftelse'")
            }
        }
    }

}

