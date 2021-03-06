package no.nav.veilarbvedtaksstotte.client.person

import no.nav.veilarbvedtaksstotte.utils.JsonUtils.createNoDataStr
import no.nav.common.utils.AuthUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class VeilarbpersonClientImpl(private val veilarbpersonUrl: String, private val userTokenSupplier: Supplier<String>) :
    VeilarbpersonClient {

    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentPersonNavn(fnr: String): PersonNavn {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/person/navn?fnr=$fnr"))
            .header(HttpHeaders.AUTHORIZATION, AuthUtils.bearerToken(userTokenSupplier.get()))
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun hentCVOgJobbprofil(fnr: String): String {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/person/cv_jobbprofil?fnr=$fnr"))
            .header(HttpHeaders.AUTHORIZATION, AuthUtils.bearerToken(userTokenSupplier.get()))
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            val responseBody = response.body()
            return if (response.code() == 403 || response.code() == 401) {
                return createNoDataStr("Bruker har ikke delt CV/jobbprofil med NAV")
            } else if (response.code() == 204 || response.code() == 404 || responseBody == null) {
                createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil")
            } else {
                responseBody.string()
            }
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbpersonUrl, "/internal/isAlive"), client)
    }

}
