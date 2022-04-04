package no.nav.veilarbvedtaksstotte.client.regoppslag

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.AuthUtils
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders

class RegoppslagClientImpl(
    val reguppslagUrl: String,
    val systemUserTokenProvider: SystemUserTokenProvider
) : RegoppslagClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun hentPostadresse(dto: RegoppslagRequestDTO): RegoppslagResponseDTO {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(reguppslagUrl, "/rest/postadresse"))
            .header(HttpHeaders.AUTHORIZATION, AuthUtils.bearerToken(systemUserTokenProvider.getSystemUserToken()))
            .post(dto.toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(reguppslagUrl, "isReady"), client)
    }
}
