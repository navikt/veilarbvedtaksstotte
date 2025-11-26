package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.JaNeiVetIkke

/**
 * Har personen andre forhold som hindrer dem i å jobbe?
 * @param andreForholdHindrerArbeid 
 */
data class Annet(

    @get:JsonProperty("andreForholdHindrerArbeid") val andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
) {

}

