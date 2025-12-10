package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Har personen andre forhold som hindrer dem i å jobbe?
 * @param andreForholdHindrerArbeid
 */
data class Annet(

    @get:JsonProperty("andreForholdHindrerArbeid") val andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)

