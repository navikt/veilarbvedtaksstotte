package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime

/**
 *
 * @param type
 * @param id Unik identifikator for opplysningene
 * @param sendtInnAv
 * @param utdanning
 * @param helse
 * @param jobbsituasjon
 * @param annet
 */
data class OpplysningerOmArbeidssoeker(

    @get:JsonProperty("type", required = true) val type: Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("utdanning") val utdanning: Utdanning? = null,

    @get:JsonProperty("helse") val helse: Helse? = null,

    @get:JsonProperty("jobbsituasjon") val jobbsituasjon: Jobbsituasjon? = null,

    @get:JsonProperty("annet") val annet: Annet? = null,

    @get:JsonProperty("tidspunkt", required = true) val tidspunkt: LocalDateTime
) {

    /**
     *
     * Values: OPPLYSNINGER_V4
     */
    enum class Type(@get:JsonValue val value: String) {

        OPPLYSNINGER_V4("OPPLYSNINGER_V4");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'OpplysningerOmArbeidssoeker'")
            }
        }
    }

}

