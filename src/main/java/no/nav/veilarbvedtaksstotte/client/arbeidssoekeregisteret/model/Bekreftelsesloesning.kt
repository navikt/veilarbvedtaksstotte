package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: UKJENT_VERDI,ARBEIDSSOEKERREGISTERET,DAGPENGER,FRISKMELDT_TIL_ARBEIDSFORMIDLING
*/
enum class Bekreftelsesloesning(@get:JsonValue val value: kotlin.String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    ARBEIDSSOEKERREGISTERET("ARBEIDSSOEKERREGISTERET"),
    DAGPENGER("DAGPENGER"),
    FRISKMELDT_TIL_ARBEIDSFORMIDLING("FRISKMELDT_TIL_ARBEIDSFORMIDLING");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Bekreftelsesloesning {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Bekreftelsesloesning'")
        }
    }
}

