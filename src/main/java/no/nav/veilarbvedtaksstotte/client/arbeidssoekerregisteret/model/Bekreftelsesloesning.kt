package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * Values: UKJENT_VERDI,ARBEIDSSOEKERREGISTERET,DAGPENGER,FRISKMELDT_TIL_ARBEIDSFORMIDLING
 */
enum class Bekreftelsesloesning(@get:JsonValue val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    ARBEIDSSOEKERREGISTERET("ARBEIDSSOEKERREGISTERET"),
    DAGPENGER("DAGPENGER"),
    FRISKMELDT_TIL_ARBEIDSFORMIDLING("FRISKMELDT_TIL_ARBEIDSFORMIDLING");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Bekreftelsesloesning {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Bekreftelsesloesning'")
        }
    }
}

