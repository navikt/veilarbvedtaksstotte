package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param sendtInnAv
 * @param gjelderFra
 * @param gjelderTil
 * @param harJobbetIDennePerioden
 * @param vilFortsetteSomArbeidssoeker
 */
data class Svar(

    @get:JsonProperty("sendtInnAv", required = true) val sendtInnAv: Metadata,

    @get:JsonProperty("gjelderFra", required = true) val gjelderFra: java.time.OffsetDateTime,

    @get:JsonProperty("gjelderTil", required = true) val gjelderTil: java.time.OffsetDateTime,

    @get:JsonProperty("harJobbetIDennePerioden", required = true) val harJobbetIDennePerioden: Boolean,

    @get:JsonProperty("vilFortsetteSomArbeidssoeker", required = true) val vilFortsetteSomArbeidssoeker: Boolean
)

