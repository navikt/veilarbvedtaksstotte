package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime

/**
 * Inneholder data om en startet periode
 * @param type
 * @param tidspunkt Tidspunkt for endringen
 * @param utfoertAv
 * @param kilde Navn på systemet som utførte endringen eller ble benyttet til å utføre endringen
 * @param aarsak Aarasek til endringen. Feks \"Flyttet ut av landet\" eller lignende
 * @param tidspunktFraKilde
 */
data class PeriodeAvluttet(

    @get:JsonProperty("type", required = true) val type: Type,

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("tidspunkt", required = true) val tidspunkt: LocalDateTime
) {

    /**
     *
     * Values: PERIODE_AVSLUTTET_V1
     */
    enum class Type(@get:JsonValue val value: String) {

        PERIODE_AVSLUTTET_V1("PERIODE_AVSLUTTET_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'PeriodeAvluttet'")
            }
        }
    }

}

