package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret


import jakarta.ws.rs.core.HttpHeaders
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.client.person.BehandlingsNummer
import no.nav.veilarbvedtaksstotte.client.person.request.PersonRequest
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier

open class OppslagArbeidssoekerregisteretClientImpl(
    private val url: String,
    private val machineToMachinetokenSupplier: Supplier<String>
) : OppslagArbeidssoekerregisteretClient {
    val client: OkHttpClient = RestClient.baseClient()
    val consumerId = "veilarbvedtaksstotte"

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(url, "/internal/isAlive"), client)
    }

    override fun hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr: Fnr): OpplysningerOmArbeidssoekerMedProfilering? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v3/person/hent-siste-opplysninger-om-arbeidssoeker-med-profilering"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachinetokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(PersonRequest(fnr, BehandlingsNummer.VEDTAKSTOTTE.value)))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            if (response.code == 404 || response.code == 204) {
                return null
            }

            return response.deserializeJsonOrThrow()
        }
    }
}