package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 
 * @param id 
 * @param type 
 * @param status 
 * @param title 
 * @param instance 
 * @param timestamp 
 * @param detail 
 */
data class ProblemDetails(

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("type", required = true) val type: java.net.URI,

    @get:JsonProperty("status", required = true) val status: kotlin.Int,

    @get:JsonProperty("title", required = true) val title: kotlin.String,

    @get:JsonProperty("instance", required = true) val instance: kotlin.String,

    @get:JsonProperty("timestamp", required = true) val timestamp: java.time.OffsetDateTime,

    @get:JsonProperty("detail") val detail: kotlin.String? = null
) {

}

