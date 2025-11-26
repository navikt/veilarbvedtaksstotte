package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* Resultatet av en profilering UKJENT_VERDI - Verdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker. UDEFINERT - Ingen verdi er satt. ANTATT_GODE_MULIGHETER - Antatt gode muligheter for å komme i arbeid. ANTATT_BEHOV_FOR_VEILEDNING - Antatt behov for veiledning. OPPGITT_HINDRINGER - Personen har oppgitt at det finnes hindringer (helse eller annet) for å komme i arbeid. 
* Values: UKJENT_VERDI,UDEFINERT,ANTATT_GODE_MULIGHETER,ANTATT_BEHOV_FOR_VEILEDNING,OPPGITT_HINDRINGER
*/
enum class ProfilertTil(@get:JsonValue val value: kotlin.String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    UDEFINERT("UDEFINERT"),
    ANTATT_GODE_MULIGHETER("ANTATT_GODE_MULIGHETER"),
    ANTATT_BEHOV_FOR_VEILEDNING("ANTATT_BEHOV_FOR_VEILEDNING"),
    OPPGITT_HINDRINGER("OPPGITT_HINDRINGER");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ProfilertTil {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'ProfilertTil'")
        }
    }
}

