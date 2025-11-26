package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.BrukerType

/**
 * Informasjon om brukeren som utførte en handling
 * @param type 
 * @param id Identifikator for brukeren, format avhenger av brukertype
 * @param sikkerhetsnivaa Sikkerhetsnivået bruker var innlogget med ved utførelse av handlingen
 */
data class Bruker(

    @get:JsonProperty("type", required = true) val type: BrukerType,

    @get:JsonProperty("id", required = true) val id: kotlin.String,

    @get:JsonProperty("sikkerhetsnivaa") val sikkerhetsnivaa: kotlin.String? = null
) {

}

