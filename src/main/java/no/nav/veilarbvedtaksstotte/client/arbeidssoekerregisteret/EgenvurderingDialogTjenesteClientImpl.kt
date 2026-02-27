package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJson
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.*
import java.util.function.Supplier

interface EgenvurderingDialogTjenesteClient {
    fun hentDialogId(arbeidssokerperiodeId: UUID): EgenvurderingDialogResponse?
}

class EgenvurderingDialogTjenesteClientImpl (
    private val url: String,
    private val machineToMachineTokenClient: Supplier<String>
) : EgenvurderingDialogTjenesteClient {
    private val client: OkHttpClient = RestClient.baseClient()
    private val log = LoggerFactory.getLogger(EgenvurderingDialogTjenesteClientImpl::class.java)

    override fun hentDialogId(arbeidssokerperiodeId: UUID): EgenvurderingDialogResponse? {
        val request = Request.Builder()
            .url("$url/api/v1/egenvurdering/dialog")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.get()}")
            .post(EgenvurderingDialogRequest(arbeidssokerperiodeId).toJson().toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message =
                    "Uventet status ${response.code} ved kall mot mot ${response.request.url}"
                log.warn(message)
                log.warn("Klarte ikke hente dialogId for arbeidssokerperiodeId=$arbeidssokerperiodeId")
                throw RuntimeException(message)
            }

            return response.deserializeJson()
        }
    }
}

data class EgenvurderingDialogRequest(val periodeId: UUID)

data class EgenvurderingDialogResponse(@param:NotNull val dialogId: Long)
