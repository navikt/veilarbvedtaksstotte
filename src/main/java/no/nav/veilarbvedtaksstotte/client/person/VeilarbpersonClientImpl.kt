package no.nav.veilarbvedtaksstotte.client.person

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.AuthUtils.bearerToken
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto
import no.nav.veilarbvedtaksstotte.client.person.dto.CvErrorStatus
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn
import no.nav.veilarbvedtaksstotte.client.person.request.PersonRequest
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.function.Supplier

class VeilarbpersonClientImpl(private val veilarbpersonUrl: String, private val userTokenSupplier: Supplier<String>,
                              private val machineToMachineTokenSupplier: Supplier<String>) :
    VeilarbpersonClient {

    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentPersonNavn(fnr: String): PersonNavn {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/hent-navn"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(
                PersonRequest(Fnr.of(fnr), BehandlingsNummer.VEDTAKSTOTTE.value).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun hentCVOgJobbprofil(fnr: String): CvDto {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/hent-cv_jobbprofil"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(
                PersonRequest(Fnr.of(fnr), BehandlingsNummer.VEDTAKSTOTTE.value).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        RestClient.baseClient().newCall(request).execute().use { response ->
            val responseBody = response.body
            if (response.code == 403 || response.code == 401) {
                return CvDto.CvMedError(CvErrorStatus.IKKE_DELT)
            } else if (response.code == 204 || response.code == 404 || responseBody == null) {
                return CvDto.CvMedError(CvErrorStatus.IKKE_FYLT_UT)
            } else {
                return CvDto.CVMedInnhold(JsonUtils.fromJson(responseBody.string(), CvInnhold::class.java))
            }
        }
    }

    override fun hentMålform(fnr: Fnr): Målform {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "api/v3/person/hent-malform"))
            .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
            .post(
                PersonRequest(fnr, BehandlingsNummer.VEDTAKSTOTTE.value).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                return response
                    .deserializeJsonOrThrow<MalformRespons>()
                    .tilMålform()
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    data class MalformRespons(val malform: String?) {
        fun tilMålform(): Målform {
            return Målform.values().find { it.name == malform?.uppercase() } ?: Målform.NB
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbpersonUrl, "/internal/isAlive"), client)
    }

}
