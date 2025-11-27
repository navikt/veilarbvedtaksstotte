package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Profilering av arbeidssøker Inneholder informasjon som brukes til ruting av arbeidssøker til riktig veiledningstjeneste. Profileringen er koblet til et bestemt sett opplysninger om arbeidssøker og en bestemt periode. I tilfeller hvor 'opplysningerOmAbreidssøker' oppdateres til å gjelde periode vil det dukke opp en ny profilering knyttet til den samme 'opplysningerOmArbeidssokerId' (periodeId vil være endret).
 * @param type
 * @param id Unik id for profileringen
 * @param opplysningerOmArbeidssokerId Unik id for OpplysningerOmArbeidssøker som profileringen tilhører
 * @param sendtInnAv
 * @param profilertTil
 * @param jobbetSammenhengendeSeksAvTolvSisteMnd Om personen har jobbet sammenhengende seks av de siste tolv månedene
 * @param alder Personens alder
 */
data class Profilering(

    @get:JsonProperty("type", required = true) val type: Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("opplysningerOmArbeidssokerId", required = true) val opplysningerOmArbeidssokerId: java.util.UUID,

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("profilertTil", required = true) val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @get:JsonProperty(
        "jobbetSammenhengendeSeksAvTolvSisteMnd",
        required = true
    ) val jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean,

    @get:JsonProperty("alder") val alder: Int? = null
) {

    /**
     *
     * Values: PROFILERING_V1
     */
    enum class Type(@get:JsonValue val value: String) {

        PROFILERING_V1("PROFILERING_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Profilering'")
            }
        }
    }

}

