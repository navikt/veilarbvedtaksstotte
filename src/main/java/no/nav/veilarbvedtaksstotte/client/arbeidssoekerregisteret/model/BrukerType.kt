package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * Values: SLUTTBRUKER,VEILEDER,SYSTEM,UDEFINERT,UKJENT_VERDI
 */
enum class BrukerType(@get:JsonValue val value: String) {

    SLUTTBRUKER("SLUTTBRUKER"),
    VEILEDER("VEILEDER"),
    SYSTEM("SYSTEM"),
    UDEFINERT("UDEFINERT"),
    UKJENT_VERDI("UKJENT_VERDI");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): BrukerType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'BrukerType'")
        }
    }
}

