package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.JaNeiVetIkke

/**
 * Har personen helseutfordringer som hindrer dem i å jobbe?
 * @param helsetilstandHindrerArbeid 
 */
data class Helse(

    @get:JsonProperty("helsetilstandHindrerArbeid") val helsetilstandHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
) {

}

