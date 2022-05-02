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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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
            .post(dto.toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            throwIfNotSuccessful(response)
            return try {
                response.body.use { responseBody ->
                    return responseBody
                        ?.string()
                        ?.let {
                            log.info("Respons fra distribuerjournalpost: $it")
                            JsonUtils.objectMapper.readValue(it, DistribuerJournalpostResponsDTO::class.java)
                        }
                }
            } catch (e: Exception) {
                log.error("Klarte ikke lese respons", e)
                null
            }
        }
    }

    fun throwIfNotSuccessful(response: Response) {
        if (response.code == HttpStatus.CONFLICT.value()) {
            log.warn(
                "Status 409 i respons fra distribuerjournalpost: Vedtaket er allerede distribuert. " +
                        "Forsøker å lagre bestillingsId fra respons."
            )
        } else {
            RestUtils.throwIfNotSuccessful(response)
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokdistribusjonUrl, "isReady"), client)
    }
}
