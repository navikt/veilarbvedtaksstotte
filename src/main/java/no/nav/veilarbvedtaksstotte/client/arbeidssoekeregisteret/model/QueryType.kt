package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: PERIODER,IDENTITETSNUMMER
*/
enum class QueryType(@get:JsonValue val value: kotlin.String) {

    PERIODER("PERIODER"),
    IDENTITETSNUMMER("IDENTITETSNUMMER");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): QueryType {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'QueryType'")
        }
    }
}

