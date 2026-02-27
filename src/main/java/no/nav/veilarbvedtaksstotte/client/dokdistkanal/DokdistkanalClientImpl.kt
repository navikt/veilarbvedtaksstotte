package no.nav.veilarbvedtaksstotte.client.dokdistkanal

import lombok.extern.slf4j.Slf4j
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.AuthUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto.BestemDistribusjonskanalDTO
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto.BestemDistribusjonskanalResponseDTO
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonAndThrowOnNull
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

@Slf4j
class DokdistkanalClientImpl(
    val dokdistkanalUrl: String,
    val machineToMachineTokenClient: Supplier<String>
) : DokdistkanalClient {
    val log = LoggerFactory.getLogger(DokdistkanalClientImpl::class.java)
    val client: OkHttpClient = RestClient.baseClient()

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokdistkanalUrl, "/actuator/health/readiness"), client)
    }

    override fun bestemDistribusjonskanal(brukerFnr: Fnr): BestemDistribusjonskanalResponseDTO {
        val request = Request.Builder()
            .url(joinPaths(dokdistkanalUrl, "/rest/bestemDistribusjonskanal"))
            .header(HttpHeaders.AUTHORIZATION, AuthUtils.bearerToken(machineToMachineTokenClient.get()))
            .post(BestemDistribusjonskanalDTO(brukerFnr.get()).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonAndThrowOnNull()
        }
    }
}