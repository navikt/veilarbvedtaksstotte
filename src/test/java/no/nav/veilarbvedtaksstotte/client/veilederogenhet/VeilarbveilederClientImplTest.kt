package no.nav.veilarbvedtaksstotte.client.veilederogenhet

import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils.createAuthContext
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VeilarbveilederClientImplTest {
    val veilederIdent = "123"
    val veilederNavn = "Veileder Navn"

    lateinit var authContextHolder: AuthContextHolder
    lateinit var veilederClient: VeilarbveilederClient

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        JsonUtils.init()
        authContextHolder = AuthContextHolderThreadLocal.instance()
        veilederClient = VeilarbveilederClientImpl("http://localhost:" + getWireMockRule().port(),authContextHolder)
    }

    @Test
    fun henter_navn_pa_veileder() {
        val json =
            """{
             "ident": "$veilederIdent",
             "navn": "$veilederNavn"
            }"""

        givenWiremockOkJsonResponse("/api/veileder/$veilederIdent", json)
        val veilederNavnResponse = authContextHolder.withContext(createAuthContext(UserRole.INTERN, "test"), UnsafeSupplier {
            veilederClient.hentVeileder(veilederIdent)
        })

        Assert.assertEquals(Veileder(veilederIdent, veilederNavn), veilederNavnResponse)
    }
}
