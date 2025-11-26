package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 
 * @param type 
 * @param perioder 
 */
data class PerioderQueryRequest(

    @get:JsonProperty("type", required = true) val type: PerioderQueryRequest.Type,

    @get:JsonProperty("perioder", required = true) val perioder: kotlin.collections.List<java.util.UUID>
) {

    /**
    * 
    * Values: PERIODER
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        PERIODER("PERIODER");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'PerioderQueryRequest'")
            }
        }
    }

}

