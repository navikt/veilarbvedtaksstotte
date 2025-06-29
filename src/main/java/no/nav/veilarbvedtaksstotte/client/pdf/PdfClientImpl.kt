package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildePdfTemplate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class PdfClientImpl(val pdfGenUrl: String) : PdfClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun genererPdf(brevdata: BrevdataDto): ByteArray {

        val request = Request.Builder()
            .url(joinPaths(pdfGenUrl, "api/v1/genpdf/vedtak14a/vedtak14a"))
            .post(RestUtils.toJsonRequestBody(brevdata))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                val body = response.body
                return if (body != null) body.bytes() else
                    throw IllegalStateException("Generering av brev feilet, tom respons.")
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun genererOyeblikksbildeCvPdf(
        cvOyeblikksbildeData: CvInnholdMedMottakerDto,
    ): ByteArray {
        val request = Request.Builder()
            .url(joinPaths(pdfGenUrl, "api/v1/genpdf/vedtak14a/" + OyeblikksbildePdfTemplate.CV_OG_JOBBPROFIL.templateName))
            .post(RestUtils.toJsonRequestBody(cvOyeblikksbildeData))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                val body = response.body
                return if (body != null) body.bytes() else
                    throw IllegalStateException("Generering av øyeblikkbilde feilet, tøm respons.")
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData: EgenvurderingMedMottakerDto): ByteArray {
        val request = Request.Builder()
            .url(joinPaths(pdfGenUrl, "api/v1/genpdf/vedtak14a/" + OyeblikksbildePdfTemplate.EGENVURDERING.templateName))
            .post(RestUtils.toJsonRequestBody(egenvurderingOyeblikksbildeData))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                val body = response.body
                return if (body != null) body.bytes() else
                    throw IllegalStateException("Generering av øyeblikkbilde feilet, tøm respons.")
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun genererOyeblikksbildeArbeidssokerRegistretPdf(registreringOyeblikksbildeData: OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto): ByteArray {
        val request = Request.Builder()
            .url(joinPaths(pdfGenUrl, "api/v1/genpdf/vedtak14a/" + OyeblikksbildePdfTemplate.ARBEIDSSOKERREGISTRET.templateName))
            .post(RestUtils.toJsonRequestBody(registreringOyeblikksbildeData))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                RestUtils.throwIfNotSuccessful(response)
                val body = response.body
                return if (body != null) body.bytes() else
                    throw IllegalStateException("Generering av øyeblikkbilde feilet, tøm respons.")
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot " + request.url.toString(),
                e
            )
        }
    }

    override fun checkHealth(): HealthCheckResult? {
        return HealthCheckUtils.pingUrl(joinPaths(pdfGenUrl, "/internal/is_alive"), client)
    }
}
