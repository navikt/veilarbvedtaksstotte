package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

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

    @get:JsonProperty("status", required = true) val status: Int,

    @get:JsonProperty("title", required = true) val title: String,

    @get:JsonProperty("instance", required = true) val instance: String,

    @get:JsonProperty("timestamp", required = true) val timestamp: LocalDateTime,

    @get:JsonProperty("detail") val detail: String? = null
)

