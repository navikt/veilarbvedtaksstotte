package no.nav.veilarbvedtaksstotte.client.dokarkiv

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.AuthUtils.bearerToken
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.http.HttpHeaders

class DokarkivClientImpl(val dokarkivUrl: String,
                         val systemUserTokenProvider: SystemUserTokenProvider) : DokarkivClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun opprettJournalpost(opprettJournalpostDTO: OpprettJournalpostDTO): OpprettetJournalpostDTO {
        val request = Request.Builder()
                .url(joinPaths(dokarkivUrl, "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .header("Nav-Consumer-Token", bearerToken(systemUserTokenProvider.getSystemUserToken()))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RequestBody.create(RestUtils.MEDIA_TYPE_JSON, opprettJournalpostDTO.toJson()))
                .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokarkivUrl, "isReady"), client)
    }
}
