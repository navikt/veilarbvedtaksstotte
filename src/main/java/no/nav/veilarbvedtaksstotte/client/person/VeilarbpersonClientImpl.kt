package no.nav.veilarbvedtaksstotte.client.person

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.AuthUtils.bearerToken
import no.nav.common.utils.UrlUtils
import no.nav.veilarbvedtaksstotte.client.person.dto.*
import no.nav.veilarbvedtaksstotte.client.person.request.PersonRequest
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonAndThrowOnNull
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.annotations.NotNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.function.Supplier

class VeilarbpersonClientImpl(
    private val veilarbpersonUrl: String,
    private val userTokenSupplier: Supplier<String>,
    private val machineToMachineTokenSupplier: Supplier<String>
) :
    VeilarbpersonClient {

    private val client: OkHttpClient = RestClient.baseClient()
    val consumerId = "veilarbvedtaksstotte"

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
            return response.deserializeJsonAndThrowOnNull()
        }
    }

    /*
        2025-04-01, Sondre

        Denne funksjonen er helt lik hentPersonNavn, med unntak av en ting: den bruker system-til-system token supplier
        i stedet for user token supplier. Vi gjorde dette på enkleste mulig måte når vi trengte å fikse en feil der
        en batch-jobb skulle hente personen sitt navn. Da har vi ikke bruker i kontekst og kan derfor ikke bruke user token supplier.

        TODO: Dette kan gjøres "bedre" med å f.eks. kunne spesifisere hvilken type token supplier man ønsker ved call-site.
     */
    override fun hentPersonNavnForJournalforing(fnr: String): PersonNavn {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/hent-navn"))
            .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
            .post(
                PersonRequest(Fnr.of(fnr), BehandlingsNummer.VEDTAKSTOTTE.value).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()
        RestClient.baseClient().newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonAndThrowOnNull()
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

    override fun hentMalform(fnr: Fnr): Malform {
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
                    .deserializeJsonAndThrowOnNull<MalformRespons>()
                    .tilMalform()
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun hentAdressebeskyttelse(fnr: Fnr): Adressebeskyttelse {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "api/v3/person/hent-adressebeskyttelse"))
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
                    .deserializeJsonAndThrowOnNull<Adressebeskyttelse>()
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun hentFodselsdato(fnr: Fnr): FodselsdatoOgAr {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "api/v3/person/hent-foedselsdato"))
            .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
            .post(
                PersonRequest(fnr, BehandlingsNummer.VEDTAKSTOTTE.value).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                return response.deserializeJsonAndThrowOnNull<FodselsdatoOgAr>()
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }

    }

    override fun hentSisteOpplysningerOmArbeidssoekerMedProfilering(@NotNull fnr: Fnr): OpplysningerOmArbeidssoekerMedProfilering? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(veilarbpersonUrl, "/api/v3/person/hent-siste-opplysninger-om-arbeidssoeker-med-profilering"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(PersonRequest(fnr, BehandlingsNummer.VEDTAKSTOTTE.value)))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)

                if (response.code == 404 || response.code == 204) {
                    return null
                }

                return response.deserializeJsonAndThrowOnNull()
            }
        } catch (e: Exception){
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot arbeidssøkerregistret "
            )
        }
    }

    data class MalformRespons(val malform: String?) {
        fun tilMalform(): Malform {
            return Malform.values().find { it.name == malform?.uppercase() } ?: Malform.NB
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbpersonUrl, "/internal/isAlive"), client)
    }
}
