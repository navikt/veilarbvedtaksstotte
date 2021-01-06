package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.http.HttpHeaders

class VeilarbdokumentClientImpl(private val veilarbdokumentUrl: String) : VeilarbdokumentClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun sendDokument(sendDokumentDTO: SendDokumentDTO): DokumentSendtDTO {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbdokumentUrl, "/api/bestilldokument"))
            .header(HttpHeaders.AUTHORIZATION, RestClientUtils.authHeaderMedInnloggetBruker())
            .post(RequestBody.create(MEDIA_TYPE_JSON, sendDokumentDTO.toJson()))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun produserDokumentUtkast(sendDokumentDTO: SendDokumentDTO): ByteArray {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbdokumentUrl, "/api/dokumentutkast"))
            .header(HttpHeaders.AUTHORIZATION, RestClientUtils.authHeaderMedInnloggetBruker())
            .post(RequestBody.create(MEDIA_TYPE_JSON, sendDokumentDTO.toJson()))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            val body = response.body()
            return if (body != null) body.bytes() else
                throw IllegalStateException("Generering av dokumentutkast feilet, tom respons.")
        }
    }

    override fun produserDokumentV2(produserDokumentV2DTO: ProduserDokumentV2DTO): ByteArray {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbdokumentUrl, "/api/v2/produserdokument"))
            .header(HttpHeaders.AUTHORIZATION, RestClientUtils.authHeaderMedInnloggetBruker())
            .post(RequestBody.create(MEDIA_TYPE_JSON, produserDokumentV2DTO.toJson()))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            val body = response.body()
            return if (body != null) body.bytes() else
                throw IllegalStateException("Generering av dokument feilet, tom respons.")
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbdokumentUrl, "/internal/health/readiness"), client)
    }
}
