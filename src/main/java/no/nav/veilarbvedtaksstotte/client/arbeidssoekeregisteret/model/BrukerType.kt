package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: SLUTTBRUKER,VEILEDER,SYSTEM,UDEFINERT,UKJENT_VERDI
*/
enum class BrukerType(@get:JsonValue val value: kotlin.String) {

    SLUTTBRUKER("SLUTTBRUKER"),
    VEILEDER("VEILEDER"),
    SYSTEM("SYSTEM"),
    UDEFINERT("UDEFINERT"),
    UKJENT_VERDI("UKJENT_VERDI");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): BrukerType {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'BrukerType'")
        }
    }
}

