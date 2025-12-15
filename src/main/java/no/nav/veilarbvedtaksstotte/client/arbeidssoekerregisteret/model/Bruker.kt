package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Informasjon om brukeren som utførte en handling
 * @param type
 * @param id Identifikator for brukeren, format avhenger av brukertype
 * @param sikkerhetsnivaa Sikkerhetsnivået bruker var innlogget med ved utførelse av handlingen
 */
data class Bruker(

    @get:JsonProperty("type", required = true) val type: BrukerType,

    @get:JsonProperty("id", required = true) val id: String,

    @get:JsonProperty("sikkerhetsnivaa") val sikkerhetsnivaa: String? = null
)

