package no.nav.veilarbvedtaksstotte.client.veilederogenhet

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class VeilarbveilederClientImplTest {
    val veilederIdent = "123"
    val veilederNavn = "Veileder Navn"

    companion object {

        val authContextHolder = AuthContextHolderThreadLocal.instance()
        lateinit var veilederClient: VeilarbveilederClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            JsonUtils.init()
            veilederClient =
                VeilarbveilederClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort, authContextHolder,{""},
                    {""} )
        }
    }

    @Test
    fun henter_navn_pa_veileder() {
        val json =
            """{
             "ident": "$veilederIdent",
             "navn": "$veilederNavn"
            }"""

        TestUtils.givenWiremockOkJsonResponseForPost(
            "/api/veileder/hent-navn",
            WireMock.containing(veilederIdent),
            veilederNavn
        )
        val veilederNavnResponse = veilederClient.hentVeilederNavn(veilederIdent)

        assertEquals(veilederNavn, veilederNavnResponse)
    }
}
