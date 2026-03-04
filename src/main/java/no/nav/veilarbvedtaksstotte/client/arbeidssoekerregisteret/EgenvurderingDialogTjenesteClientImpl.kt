package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJson
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonAndThrowOnNull
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.*
import java.util.function.Supplier

interface EgenvurderingDialogTjenesteClient {
    fun hentDialogId(arbeidssokerperiodeId: UUID): EgenvurderingDialogResponse?
}

class EgenvurderingDialogTjenesteClientImpl(
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
            return when (val statusCode = response.code) {
                HttpStatus.OK.value() -> response.deserializeJsonAndThrowOnNull()
                HttpStatus.NO_CONTENT.value() -> response.deserializeJson()
                else -> throw EgenvurderingDialogTjenesteException("Klarte ikke hente dialogId for arbeidssokerperiodeId=$arbeidssokerperiodeId. Årsak: uventet HTTP-status $statusCode.")
            }
        }
    }
}

data class EgenvurderingDialogRequest(val periodeId: UUID)

data class EgenvurderingDialogResponse(@param:NotNull val dialogId: Long)

data class EgenvurderingDialogTjenesteException(override val message: String) : RuntimeException(message)
