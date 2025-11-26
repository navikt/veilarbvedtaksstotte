package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
* Beskrivelse av jobbsituasjonen. Følgende beskrivelser er definert: UKJENT_VERDI                    -       Verdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker. UDEFINERT                       -       Verdien er ikke satt. HAR_SAGT_OPP                    -       Personen har sagt opp sin stilling. HAR_BLITT_SAGT_OPP              -       Personen har blitt sagt opp fra sin stilling. ER_PERMITTERT                   -       Personen er permittert. ALDRI_HATT_JOBB                 -       Personen har aldri hatt en jobb. IKKE_VAERT_I_JOBB_SISTE_2_AAR   -       Personen har ikke vært i jobb de siste 2 årene. AKKURAT_FULLFORT_UTDANNING      -       Personen har akkurat fullført sin utdanning. USIKKER_JOBBSITUASJON           -       Personen er usikker på sin jobbsituasjon. MIDLERTIDIG_JOBB                -       Personen har en midlertidig jobb. DELTIDSJOBB_VIL_MER             -       Personen har en/flere deltidsjobber, men ønsker å jobbe mer. NY_JOBB                         -       Personen har fått seg ny jobb. KONKURS                         -       Personen har mistet jobben på grunn av konkurs. ANNET                           -       Personen har en annen jobbsituasjon. 
* Values: UKJENT_VERDI,UDEFINERT,HAR_SAGT_OPP,HAR_BLITT_SAGT_OPP,ER_PERMITTERT,ALDRI_HATT_JOBB,IKKE_VAERT_I_JOBB_SISTE_2_AAR,AKKURAT_FULLFORT_UTDANNING,VIL_BYTTE_JOBB,USIKKER_JOBBSITUASJON,MIDLERTIDIG_JOBB,DELTIDSJOBB_VIL_MER,NY_JOBB,KONKURS,ANNET
*/
enum class Beskrivelse(@get:JsonValue val value: kotlin.String) {

    UKJENT_VERDI("UKJENT_VERDI"),
    UDEFINERT("UDEFINERT"),
    HAR_SAGT_OPP("HAR_SAGT_OPP"),
    HAR_BLITT_SAGT_OPP("HAR_BLITT_SAGT_OPP"),
    ER_PERMITTERT("ER_PERMITTERT"),
    ALDRI_HATT_JOBB("ALDRI_HATT_JOBB"),
    IKKE_VAERT_I_JOBB_SISTE_2_AAR("IKKE_VAERT_I_JOBB_SISTE_2_AAR"),
    AKKURAT_FULLFORT_UTDANNING("AKKURAT_FULLFORT_UTDANNING"),
    VIL_BYTTE_JOBB("VIL_BYTTE_JOBB"),
    USIKKER_JOBBSITUASJON("USIKKER_JOBBSITUASJON"),
    MIDLERTIDIG_JOBB("MIDLERTIDIG_JOBB"),
    DELTIDSJOBB_VIL_MER("DELTIDSJOBB_VIL_MER"),
    NY_JOBB("NY_JOBB"),
    KONKURS("KONKURS"),
    ANNET("ANNET");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Beskrivelse {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Beskrivelse'")
        }
    }
}

