package no.nav.veilarbvedtaksstotte.client.arena

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSSAK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class VeilarbarenaClientImplTest {

    lateinit var veilarbarenaClient: VeilarbarenaClient

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        veilarbarenaClient = VeilarbarenaClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance())
    }

    @Test
    fun `hent oppfolgingssak gir forventet respons`() {

        val response =
            """
                {
                    "oppfolgingssakId": "$TEST_OPPFOLGINGSSAK"
                } 
            """

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo("/api/oppfolgingssak/$TEST_FNR"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )

        val oppfolgingssak = AuthContextHolderThreadLocal
            .instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbarenaClient.oppfolgingssak(Fnr.of(TEST_FNR))
            })

        assertEquals(oppfolgingssak, TEST_OPPFOLGINGSSAK)
    }

    @Test(expected = IllegalStateException::class)
    fun `hent oppfoglingssak feiler dersom respons er 204`() {

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo("/api/oppfolgingssak/$TEST_FNR"))
                .willReturn(
                    WireMock.noContent()
                )
        )

        AuthContextHolderThreadLocal
            .instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbarenaClient.oppfolgingssak(Fnr.of(TEST_FNR))
            })
    }

    @Test(expected = IllegalStateException::class)
    fun `hent oppfoglingssak feiler dersom respons er 404`() {

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo("/api/oppfolgingssak/$TEST_FNR"))
                .willReturn(
                    WireMock.notFound()
                )
        )

        AuthContextHolderThreadLocal
            .instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbarenaClient.oppfolgingssak(Fnr.of(TEST_FNR))
            })
    }
}
