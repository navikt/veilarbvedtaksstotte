package no.nav.veilarbvedtaksstotte.client.tilgangsmaskin

import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate

interface TilgangsmaskinClient {
    fun opprettEnkelttilgang(
        ansattId: String,
        brukerId: NorskIdent,
        begrunnelse: String,
        gyldigTil: LocalDate
    )
}

class TilgangsmaskinClientImpl(
    private val url: String
) : TilgangsmaskinClient {
    private val client: OkHttpClient = RestClient.baseClient()

    override fun opprettEnkelttilgang(
        ansattId: String,
        brukerId: NorskIdent,
        begrunnelse: String,
        gyldigTil: LocalDate
    ) {
        val request = Request.Builder()
            .url(joinPaths(url, "/dev/enkelt/$ansattId"))
            .post(
                EnkeltTilgangData(brukerId.get(), begrunnelse, gyldigTil.toString())
                    .toJson()
                    .toRequestBody(RestUtils.MEDIA_TYPE_JSON)
            )
            .build()

        client.newCall(request).execute().use(RestUtils::throwIfNotSuccessful)
    }
}

data class EnkeltTilgangData(
    val brukerId: String,
    val begrunnelse: String,
    val gyldigtil: String
)
