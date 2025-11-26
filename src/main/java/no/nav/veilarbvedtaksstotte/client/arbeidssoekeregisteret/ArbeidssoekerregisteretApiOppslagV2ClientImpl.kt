package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret

import no.nav.common.health.HealthCheck
import no.nav.common.health.HealthCheckResult
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

interface ArbeidssoekerregisteretApiOppslagV2Client {
    fun hentEgenvurdering(norskIdent: NorskIdent)
}

class ArbeidssoekerregisteretApiOppslagV2ClientImpl(
    private val arbRegOppslagUrl: String,
    private val userTokenSupplier: Supplier<String>
) : ArbeidssoekerregisteretApiOppslagV2Client {
    /*
   Her er eksempelet fra Swagger, https://oppslag-v2-arbeidssoekerregisteret.intern.dev.nav.no/api/docs/v3
   Hvordan ignorerer vi resten av jsonen?
   {
   ...
       "egenvurdering": {
       "type": "EGENVURDERING_V1",
       "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
       "profileringId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
       "sendtInnAv": {
         "tidspunkt": "2025-11-24T14:43:12.474Z",
         "utfoertAv": {
           "type": "SLUTTBRUKER",
           "id": "string",
           "sikkerhetsnivaa": "string"
         },
         "kilde": "string",
         "aarsak": "string",
         "tidspunktFraKilde": {
           "tidspunkt": "2025-11-24T14:43:12.474Z",
           "avviksType": "UKJENT_VERDI"
         }
       },
       "profilertTil": "UKJENT_VERDI",
       "egenvurdering": "UKJENT_VERDI"
       },
   ...
   }
    */

    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentEgenvurdering(norskIdent: NorskIdent) {
        val request = Request.Builder()
            .url(joinPaths(arbRegOppslagUrl, "/api/v3/snapshot"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(IdentitetsnummerQueryRequest.toIdentitetsnummerQueryRequest(norskIdent).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
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


data class IdentitetsnummerQueryRequest(
    val type: String = "IDENTITETSNUMMER",
    val identitetsnummer: String
) {
    companion object {
        fun toIdentitetsnummerQueryRequest(norskIdent: NorskIdent): IdentitetsnummerQueryRequest {
            return IdentitetsnummerQueryRequest(identitetsnummer = norskIdent.get())
        }
    }
}

