package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Har personen helseutfordringer som hindrer dem i å jobbe?
 * @param helsetilstandHindrerArbeid
 */
data class Helse(

    @get:JsonProperty("helsetilstandHindrerArbeid") val helsetilstandHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)

