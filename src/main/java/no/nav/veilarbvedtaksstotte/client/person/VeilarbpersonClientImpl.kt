package no.nav.veilarbvedtaksstotte.client.person

import no.nav.veilarbvedtaksstotte.utils.JsonUtils.createNoDataStr
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.client.PersonRequest
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.function.Supplier

class VeilarbpersonClientImpl(private val veilarbpersonUrl: String, private val userTokenSupplier: Supplier<String>) :
    VeilarbpersonClient {

    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentPersonNavn(fnr: String): PersonNavn {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/navn"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
			.post(PersonRequest(Fnr.of(fnr)).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun hentCVOgJobbprofil(fnr: String): String {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/cv_jobbprofil"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
			.post(PersonRequest(Fnr.of(fnr)).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            val responseBody = response.body
            return if (response.code == 403 || response.code == 401) {
                return createNoDataStr("Bruker har ikke delt CV/jobbprofil med NAV")
            } else if (response.code == 204 || response.code == 404 || responseBody == null) {
                createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil")
            } else {
                responseBody.string()
            }
        }
    }

    override fun hentMålform(fnr: Fnr): Målform {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "api/v3/person/malform"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
			.post(PersonRequest(fnr).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                return response
                    .deserializeJsonOrThrow<MalformRespons>()
                    .tilMålform()
            }
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url.toString(), e)
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
