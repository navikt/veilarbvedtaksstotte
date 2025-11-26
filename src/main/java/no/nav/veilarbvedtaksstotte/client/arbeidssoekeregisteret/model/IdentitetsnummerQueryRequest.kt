package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.common.types.identer.NorskIdent

/**
 * 
 * @param type 
 * @param identitetsnummer Norsk identitetsnummer (11 siffer, ingen andre tegn)
 */
data class IdentitetsnummerQueryRequest(
    val type: IdentitetsnummerQueryRequest.Type = Type.IDENTITETSNUMMER,
    val identitetsnummer: kotlin.String
) {

    /**
    * 
    * Values: IDENTITETSNUMMER
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        IDENTITETSNUMMER("IDENTITETSNUMMER");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'IdentitetsnummerQueryRequest'")
            }
        }
    }

    companion object {
        fun toIdentitetsnummerQueryRequest(norskIdent: NorskIdent): IdentitetsnummerQueryRequest {
            return IdentitetsnummerQueryRequest(identitetsnummer = norskIdent.get())
        }
    }
}

