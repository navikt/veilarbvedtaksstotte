package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Ukjent verdi settes aldri direkte, men brukes som standardverdi og for å indikere at en verdi er ukjent for mottaker av melding, dvs at at den er satt til en verdi som ikke er definert i Avro-skjemaet til mottaker.
 * Values: UKJENT_VERDI,FORSINKELSE,RETTING,SLETTET,TIDSPUNKT_KORRIGERT
 */
enum class AvviksType(@get:JsonValue val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    FORSINKELSE("FORSINKELSE"),
    RETTING("RETTING"),
    SLETTET("SLETTET"),
    TIDSPUNKT_KORRIGERT("TIDSPUNKT_KORRIGERT");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): AvviksType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'AvviksType'")
        }
    }
}

