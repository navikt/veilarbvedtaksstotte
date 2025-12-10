package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Angir om dette er en gyldig bekreftelse
 * Values: GYLDIG,UVENTET_KILDE,UTENFOR_PERIODE
 */
enum class BekreftelsStatus(@get:JsonValue val value: String) {

    GYLDIG("GYLDIG"),
    UVENTET_KILDE("UVENTET_KILDE"),
    UTENFOR_PERIODE("UTENFOR_PERIODE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): BekreftelsStatus {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'BekreftelsStatus'")
        }
    }
}

