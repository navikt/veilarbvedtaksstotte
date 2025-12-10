package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * UKJENT_VERDI - Verdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker. JA - Ja. NEI - Nei. VET_IKKE - Vet ikke.
 * Values: UKJENT_VERDI,JA,NEI,VET_IKKE
 */
enum class JaNeiVetIkke(@get:JsonValue val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    JA("JA"),
    NEI("NEI"),
    VET_IKKE("VET_IKKE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): JaNeiVetIkke {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'JaNeiVetIkke'")
        }
    }
}

