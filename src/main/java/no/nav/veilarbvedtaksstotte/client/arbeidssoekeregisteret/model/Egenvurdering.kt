package org.openapitools.model

import java.util.Locale
import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.openapitools.model.Metadata
import org.openapitools.model.ProfilertTil

/**
 * Brukers egenvurdering av sin situasjon
 * @param type 
 * @param id Unik id for egenvurderingen
 * @param profileringId Profilering id
 * @param sendtInnAv 
 * @param profilertTil 
 * @param egenvurdering 
 */
data class Egenvurdering(

    @get:JsonProperty("type", required = true) val type: Egenvurdering.Type,

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("profileringId", required = true) val profileringId: java.util.UUID,

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("profilertTil", required = true) val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @get:JsonProperty("egenvurdering", required = true) val egenvurdering: ProfilertTil = ProfilertTil.UKJENT_VERDI
) {

    /**
    * 
    * Values: EGENVURDERING_V1
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        EGENVURDERING_V1("EGENVURDERING_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Egenvurdering'")
            }
        }
    }

}

