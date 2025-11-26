package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.Beskrivelse

/**
 * Beskrivelse av et enkelt forhold som inngår i jobbsituasjonen, feks permitering eller oppsigelse
 * @param beskrivelse 
 * @param detaljer Feltene taes bare med dersom de er er tilgjengelige, i praksis må klienter støtte å motta en tom map. Andre felter enn de som er definert her kan også forekomme. Detaljer om jobbsituasjonen. Følgende nøkler er definert:         Nøkkel                      -           Verdi gjelder_fra_dato_iso8601    -       datoen jobbsituasjonen gjelder fra (eksempel 2019-01-27) gjelder_til_dato_iso8601    -       datoen jobbsituasjonen gjelder til (eksempel 2019-01-27) stilling_styrk08            -       stillingens kode i STYRK08 (eksempel \"2359\"), se SSB for mer informasjon om STYRK08.                                     Forventet for:                                     - HAR_SAGT_OPP                                     - HAR_BLITT_SAGT_OPP                                     - PERMITTERT                                     - KONKURS                                     Kan også brukes for andre beskriverlser som er knyttet til en stilling, feks \"DELTIDSJOBB_VIL_MER\" prosent                     -       prosentandel jobbstituasjonen gjelder for (feks kombinert med 'ER_PERMITTERT' eller 'DELTIDSJOBB_VIL_MER'), eksempel \"50\". siste_dag_med_loenn_iso8601 -       Siste dag det betales lønn for (feks ved oppsigelse)(eksempel 2019-01-27).                                     Enkelte kilder, inkludert migrering har brukt 'siste_dag_med_loen_iso8601' (en 'n'), enbefaler                                     derfor å normalisere til 'siste_dag_med_loenn_iso8601' (to 'n') ved lesing for å fange begge verianter. siste_arbeidsdag_iso8601    -       Siste arbeidssdag. Ikke nødvendigvis siste dag det betales lønn for, feks ved konkurs(eksempel 2019-01-27). 
 */
data class BeskrivelseMedDetaljer(

    @get:JsonProperty("beskrivelse", required = true) val beskrivelse: Beskrivelse = Beskrivelse.UKJENT_VERDI,

    @get:JsonProperty("detaljer", required = true) val detaljer: kotlin.collections.Map<kotlin.String, kotlin.String>
) {

}

