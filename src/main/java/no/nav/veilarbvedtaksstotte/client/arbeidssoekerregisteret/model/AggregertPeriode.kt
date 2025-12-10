package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * En periode er en tidsperiode hvor en bruker har vært registrert som arbeidssøker. En bruker kan ha flere perioder, og en periode kan være pågående eller avsluttet. En periode er pågående dersom \"avsluttet\" er 'null' (ikke satt).
 * @param id Unik identifikator for perioden. Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel. Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden, det samme gjelder profileringsresultater.
 * @param identitetsnummer Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
 * @param startet
 * @param avsluttet
 * @param opplysning
 * @param profilering
 * @param egenvurdering
 * @param bekreftelse
 */
data class AggregertPeriode(

    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @get:JsonProperty("identitetsnummer", required = true) val identitetsnummer: String,

    @get:JsonProperty("startet", required = true) val startet: PeriodeStartet,

    @get:JsonProperty("avsluttet") val avsluttet: PeriodeAvluttet? = null,

    @get:JsonProperty("opplysning") val opplysning: OpplysningerOmArbeidssoeker? = null,

    @get:JsonProperty("profilering") val profilering: Profilering? = null,

    @get:JsonProperty("egenvurdering") val egenvurdering: Egenvurdering? = null,

    @get:JsonProperty("bekreftelse") val bekreftelse: Bekreftelse? = null
)

