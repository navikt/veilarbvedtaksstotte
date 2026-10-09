package no.nav.veilarbvedtaksstotte.client.tilgangsmaskin

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@WireMockTest
class TilgangsmaskinClientImplTest {

    @Test
    fun `oppretter enkelttilgang for ansatt og bruker`(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val client = TilgangsmaskinClientImpl("http://localhost:${wireMockRuntimeInfo.httpPort}")

        givenThat(
            post(urlEqualTo("/dev/enkelt/Z123456"))
                .willReturn(aResponse().withStatus(200))
        )

        client.opprettEnkelttilgang(
            "Z123456",
            NorskIdent.of("12345678910"),
            "Arbeidsrettet oppfølging",
            LocalDate.of(2026, 12, 31)
        )

        verify(
            postRequestedFor(urlEqualTo("/dev/enkelt/Z123456"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "brukerId": "12345678910",
                          "begrunnelse": "Arbeidsrettet oppfølging",
                          "gyldigtil": "2026-12-31"
                        }
                        """.trimIndent()
                    )
                )
        )
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initJsonUtils() {
            JsonUtils.init()
        }
    }
}
