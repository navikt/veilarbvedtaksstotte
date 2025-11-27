package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AggregertPeriode
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.IdentitetsnummerQueryRequest.Companion.toIdentitetsnummerQueryRequest
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

interface ArbeidssoekerregisteretApiOppslagV2Client {
    fun hentEgenvurdering(norskIdent: NorskIdent): AggregertPeriode
}

class ArbeidssoekerregisteretApiOppslagV2ClientImpl(
    private val arbRegOppslagUrl: String,
    private val userTokenSupplier: Supplier<String>
) : ArbeidssoekerregisteretApiOppslagV2Client {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentEgenvurdering(norskIdent: NorskIdent): AggregertPeriode {
        val request = Request.Builder()
            .url(joinPaths(arbRegOppslagUrl, "/api/v3/snapshot"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(
                toIdentitetsnummerQueryRequest(norskIdent).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

        /* Henter siste arbeidssøkerperiode, men trenger ikke bety at den er aktiv. Vi må sjekke om "avsluttet" finnes for å bekrefte det.
           Eller må vi heller sjekke om arbeidssøkerperioden er innenfor oppfølgingsperioden?
           Hvis man har hatt en arbeidssøkerperiode som er avsluttet innenfor en oppfølgingsperiode, så kan vel fortsatt veileder bruke egenvurderingen derfra som en kilde til et (nytt) vedtak?
         */
    }
}
