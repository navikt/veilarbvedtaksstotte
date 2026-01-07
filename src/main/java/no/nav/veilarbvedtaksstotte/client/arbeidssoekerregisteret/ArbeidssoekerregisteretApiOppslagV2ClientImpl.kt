package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AggregertPeriode
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.IdentitetsnummerQueryRequest.Companion.toIdentitetsnummerQueryRequest
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.ProfilertTil
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingV2Dto
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.function.Supplier

interface ArbeidssoekerregisteretApiOppslagV2Client {
    fun hentEgenvurdering(norskIdent: NorskIdent): AggregertPeriode?
}

class ArbeidssoekerregisteretApiOppslagV2ClientImpl(
    private val arbRegOppslagUrl: String,
    private val userTokenSupplier: Supplier<String>
) : ArbeidssoekerregisteretApiOppslagV2Client {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentEgenvurdering(norskIdent: NorskIdent): AggregertPeriode? {
        val request = Request.Builder()
            .url(joinPaths(arbRegOppslagUrl, "/api/v4/snapshot"))
            .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
            .post(
                toIdentitetsnummerQueryRequest(norskIdent).toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

        /* Henter siste arbeidssøkerperiode, men trenger ikke bety at den er aktiv. Vi må sjekke om "avsluttet" finnes for å bekrefte det.
           Eller må vi heller sjekke om arbeidssøkerperioden er innenfor oppfølgingsperioden?
           Hvis man har hatt en arbeidssøkerperiode som er avsluttet innenfor en oppfølgingsperiode, så kan vel fortsatt veileder bruke egenvurderingen derfra som en kilde til et (nytt) vedtak?
         */
    }

    companion object {
        @JvmStatic
        fun mapToEgenvurderingV2Dto(aggregertPeriode: AggregertPeriode, dialogId: Long?): EgenvurderingV2Dto? {
            val egenvurdering = aggregertPeriode.egenvurdering?: return null

            val svar = when (egenvurdering.egenvurdering) {
                ProfilertTil.ANTATT_GODE_MULIGHETER -> "Jeg ønsker å klare meg selv"
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> "Jeg ønsker oppfølging fra NAV"
                else -> return null
            }

            return EgenvurderingV2Dto(
                egenvurderingId = egenvurdering.id,
                sendtInnTidspunkt = egenvurdering.sendtInnAv.tidspunkt,
                dialogId = dialogId,
                svar = svar
            )
        }
    }
}
