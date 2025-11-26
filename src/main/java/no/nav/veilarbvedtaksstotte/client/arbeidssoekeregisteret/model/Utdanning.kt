package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.JaNeiVetIkke

/**
 * Information about the job seeker's education background
 * @param nus NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB
 * @param bestaatt 
 * @param godkjent 
 */
data class Utdanning(

    @get:JsonProperty("nus", required = true) val nus: kotlin.String,

    @get:JsonProperty("bestaatt") val bestaatt: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI,

    @get:JsonProperty("godkjent") val godkjent: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
) {

}

