package no.nav.veilarbvedtaksstotte.client.aiaBackend

import no.nav.common.health.HealthCheckResult
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class AiaBackendClientImpl(private val aiaBackendUrl: String, private val userTokenSupplier: Supplier<String>) : AiaBackendClient {

    private val log: Logger = LoggerFactory.getLogger(AiaBackendClientImpl::class.java)

    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentEgenvurdering(egenvurderingForPersonRequest: EgenvurderingForPersonRequest): EgenvurderingResponseDTO? {
        val request = Request.Builder()
            .url(joinPaths(aiaBackendUrl, "/veileder/behov-for-veiledning"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(egenvurderingForPersonRequest.toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            log.debug("Behovsvurdering - responsestatus: {}", response.code)
            if (response.code == 204) {
                return null
            }
            return response.deserializeJsonOrThrow()
        }
    }

    override fun checkHealth(): HealthCheckResult {
        TODO("Not yet implemented")
    }
}
