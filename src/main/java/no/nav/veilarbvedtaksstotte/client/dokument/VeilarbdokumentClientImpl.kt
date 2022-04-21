package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders

class VeilarbdokumentClientImpl(
    private val veilarbdokumentUrl: String,
    val authContextHolder: AuthContextHolder
) : VeilarbdokumentClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun produserDokument(produserDokumentDTO: ProduserDokumentDTO): ByteArray {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbdokumentUrl, "/api/v2/produserdokument"))
            .header(HttpHeaders.AUTHORIZATION, RestClientUtils.authHeaderMedInnloggetBruker(authContextHolder))
            .post(produserDokumentDTO.toJson().toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.body?.bytes()
                ?: throw IllegalStateException("Generering av dokument feilet, tom respons.")
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbdokumentUrl, "/internal/health/readiness"), client)
    }
}
