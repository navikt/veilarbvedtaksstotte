package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* Angir om dette er en gyldig bekreftelse
* Values: GYLDIG,UVENTET_KILDE,UTENFOR_PERIODE
*/
enum class BekreftelsStatus(@get:JsonValue val value: kotlin.String) {

    GYLDIG("GYLDIG"),
    UVENTET_KILDE("UVENTET_KILDE"),
    UTENFOR_PERIODE("UTENFOR_PERIODE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): BekreftelsStatus {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'BekreftelsStatus'")
        }
    }
}

