package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import java.util.UUID
import java.util.function.Supplier

interface EgenvurderingDialogTjenesteClient {
    fun hentDialogId(arbeidssokerperiodeId: UUID): EgenvurderingDialogResponse?
}

class EgenvurderingDialogTjenesteClientImpl (
    private val url: String,
    private val machineToMachineTokenClient: Supplier<String>
) : EgenvurderingDialogTjenesteClient {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun hentDialogId(arbeidssokerperiodeId: UUID): EgenvurderingDialogResponse? {
        val request = Request.Builder()
            .url("$url/api/v1/egenvurdering/dialog")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.get()}")
            .post(EgenvurderingDialogRequest(arbeidssokerperiodeId).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }
}

data class EgenvurderingDialogRequest(val periodeId: UUID)

data class EgenvurderingDialogResponse(val dialogId: Long)
