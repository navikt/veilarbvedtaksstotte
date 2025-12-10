package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.common.types.identer.NorskIdent

/**
 *
 * @param type
 * @param identitetsnummer Norsk identitetsnummer (11 siffer, ingen andre tegn)
 */
data class IdentitetsnummerQueryRequest(
    val type: Type = Type.IDENTITETSNUMMER,
    val identitetsnummer: String
) {

    /**
     *
     * Values: IDENTITETSNUMMER
     */
    enum class Type(@get:JsonValue val value: String) {

        IDENTITETSNUMMER("IDENTITETSNUMMER");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'IdentitetsnummerQueryRequest'")
            }
        }
    }

    companion object {
        fun toIdentitetsnummerQueryRequest(norskIdent: NorskIdent): IdentitetsnummerQueryRequest {
            return IdentitetsnummerQueryRequest(identitetsnummer = norskIdent.get())
        }
    }
}

