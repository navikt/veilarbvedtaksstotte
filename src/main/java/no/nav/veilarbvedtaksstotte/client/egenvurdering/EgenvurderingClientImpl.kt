package no.nav.veilarbvedtaksstotte.client.egenvurdering

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class EgenvurderingClientImpl(private val aiaBackendUrl: String, private val userTokenSupplier: Supplier<String>) : EgenvurderingClient {

    private val log: Logger = LoggerFactory.getLogger(EgenvurderingClientImpl::class.java)

    private val client: OkHttpClient = RestClient.baseClient()
    override fun hentEgenvurdering(egenvurderingForPersonDTO: EgenvurderingForPersonDTO): EgenvurderingResponseDTO? {
        val request = Request.Builder()
            .url(joinPaths(aiaBackendUrl, "/veileder/behov-for-veiledning"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(egenvurderingForPersonDTO.toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            log.info("Behovsvurdering - responsestatus: {}", response.code)
            if (response.code == 204) {
                return null
            }
            return response.deserializeJsonOrThrow()
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(aiaBackendUrl, "/internal/isReady"), client)
    }
}
