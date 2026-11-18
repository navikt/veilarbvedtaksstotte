package no.nav.veilarbvedtaksstotte.client.veilarbportefolje

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.AuthUtils.bearerToken
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto.EtiketterForBrukerDTO
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.request.EtiketterRequest
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class VeilarbportefoljeClientImpl(
    private val veilarbportefoljeUrl: String,
    private val machineToMachineTokenSupplier: Supplier<String>
) : VeilarbportefoljeClient {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentEtiketter(fnrListe: List<NorskIdent>): List<EtiketterForBrukerDTO> {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbportefoljeUrl, "/api/v1/beslutteroversikt/hent-etiketter"))
            .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
            .post(
                EtiketterRequest(fnrListe.map { it.get() }).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return RestUtils.getBodyStr(response)
                .map { JsonUtils.fromJsonArray(it, EtiketterForBrukerDTO::class.java) }
                .orElseThrow { IllegalStateException("Unable to parse JSON array from response body") }
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbportefoljeUrl, "/internal/isAlive"), client)
    }
}
