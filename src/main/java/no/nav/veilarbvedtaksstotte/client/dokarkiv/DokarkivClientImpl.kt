package no.nav.veilarbvedtaksstotte.client.dokarkiv

import lombok.extern.slf4j.Slf4j
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.AuthUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonAndThrowOnNull
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.function.Supplier

@Slf4j
class DokarkivClientImpl(
    val dokarkivUrl: String,
    val machineToMachineTokenClient: Supplier<String>
) : DokarkivClient {

    val log: Logger = LoggerFactory.getLogger(DokarkivClientImpl::class.java)

    val client: OkHttpClient = RestClient.baseClient()

    override fun opprettJournalpost(opprettJournalpostDTO: OpprettJournalpostDTO): OpprettetJournalpostDTO {
        val request = Request.Builder()
            .url(joinPaths(dokarkivUrl, "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
            .header(HttpHeaders.AUTHORIZATION, AuthUtils.bearerToken(machineToMachineTokenClient.get()))
            .post(opprettJournalpostDTO.toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            throwIfNotSuccessful(response)
            return response.deserializeJsonAndThrowOnNull()
        }
    }

    fun throwIfNotSuccessful(response: Response) {
        if (response.code == HttpStatus.CONFLICT.value()) {
            log.warn(
                "Status 409 CONFLICT i respons fra journalpostapi: Vedtaket er allerede journalført. " +
                        "Forsøker å hente journalpost fra respons."
            )
        } else {
            RestUtils.throwIfNotSuccessful(response)
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokarkivUrl, "actuator/health/readiness"), client)
    }
}
