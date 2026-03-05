package no.nav.veilarbvedtaksstotte.klagebehandling.client

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier


interface KabalClient {
    fun sendKlageTilKabal(klageDto: KabalDTO)
}

class KabalClientImpl(
    private val url: String,
    private val machineToMachineTokenClient: Supplier<String>
) : KabalClient {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun sendKlageTilKabal(klageDto: KabalDTO) {
        val request = Request.Builder()
            .url("$url/api/oversendelse/v4/sak")
            .header("Authorization", "Bearer ${machineToMachineTokenClient.get()}")
            .post(RestUtils.toJsonRequestBody(klageDto))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message =
                    "Uventet status ${response.code} ved kall mot Kabal for klageId " +
                            "${klageDto.kildeReferanse} med melding ${response.body?.string()}"
                secureLog.error(message)
                throw RuntimeException(message)
            }
        }
    }


    // TODO - legg til healthcheck?

}
