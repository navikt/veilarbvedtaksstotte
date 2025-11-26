package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: ASC,DESC
*/
enum class SortOrder(@get:JsonValue val value: kotlin.String) {

    ASC("ASC"),
    DESC("DESC");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SortOrder {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'SortOrder'")
        }
    }
}

