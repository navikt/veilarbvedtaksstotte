package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.Annet
import org.openapitools.model.Helse
import org.openapitools.model.Jobbsituasjon
import org.openapitools.model.Metadata
import org.openapitools.model.Utdanning

/**
 * 
 * @param type 
 * @param id Unik identifikator for opplysningene
 * @param sendtInnAv 
 * @param utdanning 
 * @param helse 
 * @param jobbsituasjon 
 * @param annet 
 */
data class OpplysningerOmArbeidssoeker(

    @get:JsonProperty("type", required = true) val type: OpplysningerOmArbeidssoeker.Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("utdanning") val utdanning: Utdanning? = null,

    @get:JsonProperty("helse") val helse: Helse? = null,

    @get:JsonProperty("jobbsituasjon") val jobbsituasjon: Jobbsituasjon? = null,

    @get:JsonProperty("annet") val annet: Annet? = null
) {

    /**
    * 
    * Values: OPPLYSNINGER_V4
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        OPPLYSNINGER_V4("OPPLYSNINGER_V4");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'OpplysningerOmArbeidssoeker'")
            }
        }
    }

}

