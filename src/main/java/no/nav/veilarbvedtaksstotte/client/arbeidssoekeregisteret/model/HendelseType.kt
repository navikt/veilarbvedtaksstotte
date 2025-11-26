package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: PERIODE_STARTET_V1,PERIODE_AVSLUTTET_V1,OPPLYSNINGER_V4,PROFILERING_V1,EGENVURDERING_V1,BEKREFTELSE_V1,PAA_VEGNE_AV_START_V1,PAA_VEGNE_AV_STOPP_V1
*/
enum class HendelseType(@get:JsonValue val value: kotlin.String) {

    PERIODE_STARTET_V1("PERIODE_STARTET_V1"),
    PERIODE_AVSLUTTET_V1("PERIODE_AVSLUTTET_V1"),
    OPPLYSNINGER_V4("OPPLYSNINGER_V4"),
    PROFILERING_V1("PROFILERING_V1"),
    EGENVURDERING_V1("EGENVURDERING_V1"),
    BEKREFTELSE_V1("BEKREFTELSE_V1"),
    PAA_VEGNE_AV_START_V1("PAA_VEGNE_AV_START_V1"),
    PAA_VEGNE_AV_STOPP_V1("PAA_VEGNE_AV_STOPP_V1");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): HendelseType {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'HendelseType'")
        }
    }
}

