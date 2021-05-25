package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient
import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.client.pdl.Tema
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import javax.ws.rs.core.HttpHeaders.ACCEPT

class BrukerIdentServiceTest {
    private val wireMockRule = WireMockRule()

    lateinit var brukerIdentService: BrukerIdentService
    lateinit var unleashService: UnleashService
    lateinit var pdlClient: PdlClient
    lateinit var aktorOppslagClient: AktorOppslagClient

    val userToken = "user token"
    val consumerToken = "consumer token"

    val unleashClient: UnleashClient = mock(UnleashClient::class.java)

    @Rule
    fun getWireMockRule() = wireMockRule


    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()

        pdlClient = PdlClientImpl(wiremockUrl, Tema.GEN, { userToken }, { consumerToken })
        aktorOppslagClient = PdlAktorOppslagClient(pdlClient)
        unleashService = UnleashService(unleashClient)
        brukerIdentService = BrukerIdentService(pdlClient, aktorOppslagClient, unleashService)
    }

    @Test
    fun henterOgSetterSammenBrukerIdenterRiktig() {
        val fnr = Fnr(RandomStringUtils.randomNumeric(10))

        val graphqlJsonRequest = TestUtils.readTestResourceFile("pdl-graphql-hentIdenter-request.json")
            .replace("IDENT_PLACEHOLDER", fnr.get())
        val graphqlJsonResponse = TestUtils.readTestResourceFile("pdl-graphql-hentIdenter-response.json")

        givenThat(
            post(urlEqualTo("/graphql"))
                .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader("Authorization", equalTo("Bearer " + userToken))
                .withHeader("Nav-Consumer-Token", equalTo("Bearer " + consumerToken))
                .withHeader("Tema", equalTo("GEN"))
                .withRequestBody(equalToJson(graphqlJsonRequest))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(graphqlJsonResponse)
                )
        )

        val hentIdenter: BrukerIdenter = brukerIdentService.hentIdenter(fnr)

        val forventet = BrukerIdenter(
            fnr = Fnr("33333"),
            aktorId = AktorId("55555"),
            historiskeFnr = listOf(Fnr("22222"), Fnr("44444")),
            historiskeAktorId = listOf(AktorId("11111"))
        )

        assertEquals(forventet, hentIdenter)
    }

}
