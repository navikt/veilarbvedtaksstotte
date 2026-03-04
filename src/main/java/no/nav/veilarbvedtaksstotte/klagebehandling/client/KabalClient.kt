package no.nav.veilarbvedtaksstotte.klagebehandling.client

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.util.function.Supplier
import kotlin.code
import kotlin.text.get


interface KabalClient {
    fun sendKlageTilKabal(klageDto: KabalDTO)
}

class KabalClientImpl(
    private val url: String,
    private val machineToMachineTokenClient: Supplier<String>
) : KabalClient {
    private val client: OkHttpClient = RestClient.baseClient()
    private val log = LoggerFactory.getLogger(KabalClientImpl::class.java)

//    override fun sendKlageTilKabal(klageDto: KabalDTO) {
//        val request = Request.Builder()
//            .url("$url/api/oversendelse/v4/sak")
//            .header("Authorization", "Bearer ${machineToMachineTokenClient.get()}")
//            .post(RestUtils.toJsonRequestBody(klageDto))
//            .build()
//
//        client.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) {
//                val message =
//                    "Uventet status ${response.code} ved kall mot Kabal for klageId ${klageDto.kildeReferanse} med melding ${response.message}"
//                log.error(message)
//                throw RuntimeException(message)
//            }
//        }
//    }

    override fun sendKlageTilKabal(klageDto: KabalDTO) {
        val jsonBody = RestUtils.toJsonRequestBody(klageDto)
        log.debug("Sending to Kabal: {} - Body: {}", jsonBody.contentType(), klageDto)

        // Or to get the actual JSON string:
        val objectMapper = no.nav.common.json.JsonUtils.getMapper()
        val jsonString = objectMapper.writeValueAsString(klageDto)
        log.info("Request body: $jsonString")
        println("Request body: $jsonString")

        val request = Request.Builder()
            .url("$url/api/oversendelse/v4/sak")
            .header("Authorization", "Bearer ${machineToMachineTokenClient.get()}")
            .post(RestUtils.toJsonRequestBody(klageDto))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message =
                    "Uventet status ${response.code} ved kall mot Kabal for klageId ${klageDto.kildeReferanse} " +
                            "med melding ${response.message} og json $jsonBody"
                log.error(message)
                throw RuntimeException(message)
            }
        }
    }


    // TODO - legg til healthcheck?

}
