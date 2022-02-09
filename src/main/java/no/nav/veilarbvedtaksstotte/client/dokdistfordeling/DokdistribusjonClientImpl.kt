package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class DokdistribusjonClientImpl(
    private val dokdistribusjonUrl: String,
    private val serviceTokenSupplier: Supplier<String>
) : DokdistribusjonClient {

    val log = LoggerFactory.getLogger(DokdistribusjonClientImpl::class.java)

    val client: OkHttpClient = RestClient.baseClient()

    override fun distribuerJournalpost(dto: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO? {
        val request = Request.Builder()
            .url(joinPaths(dokdistribusjonUrl, "/rest/v1/distribuerjournalpost"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenSupplier.get())
            .post(RequestBody.create(RestUtils.MEDIA_TYPE_JSON, dto.toJson()))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return try {
                response.body().use { responseBody ->
                    return responseBody
                        ?.string()
                        ?.let {
                            log.info("Respons fra distribuerjournalpost: $it")
                            JsonUtils.objectMapper.readValue(it, DistribuerJournalpostResponsDTO::class.java)
                        }
                }
            } catch (e: RuntimeException) {
                null
            }
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokdistribusjonUrl, "isReady"), client)
    }
}
