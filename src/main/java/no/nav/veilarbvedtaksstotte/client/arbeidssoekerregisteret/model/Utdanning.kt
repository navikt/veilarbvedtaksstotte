package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Information about the job seeker's education background
 * @param nus NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB
 * @param bestaatt
 * @param godkjent
 */
data class Utdanning(

    @get:JsonProperty("nus", required = true) val nus: String,

    @get:JsonProperty("bestaatt") val bestaatt: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI,

    @get:JsonProperty("godkjent") val godkjent: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)

