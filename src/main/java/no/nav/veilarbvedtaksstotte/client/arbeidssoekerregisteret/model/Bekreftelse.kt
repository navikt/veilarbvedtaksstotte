package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Bekreftelse melding
 * @param type
 * @param id Unik id for meldingen. Duplikater sees på som nettverkshikke eller lignende og skal trygt kunne ignoreres
 * @param bekreftelsesloesning
 * @param status
 * @param svar
 */
data class Bekreftelse(

    @get:JsonProperty("type", required = true) val type: Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty(
        "bekreftelsesloesning",
        required = true
    ) val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("status", required = true) val status: BekreftelsStatus,

    @get:JsonProperty("svar", required = true) val svar: Svar
) {

    /**
     *
     * Values: BEKREFTELSE_V1
     */
    enum class Type(@get:JsonValue val value: String) {

        BEKREFTELSE_V1("BEKREFTELSE_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Bekreftelse'")
            }
        }
    }

}

