package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.abac.AbacClient
import no.nav.common.abac.Pep
import no.nav.common.abac.XacmlMapper
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.test.auth.AuthTestUtils.createAuthContext
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.Credentials
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilNavEnhetPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils.assertThrowsWithMessage
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.*

class AuthServiceTest {
    var authContextHolder = AuthContextHolderThreadLocal.instance()
    var aktorOppslagClient = mock(AktorOppslagClient::class.java)
    var pep = mock(Pep::class.java)
    var veilarbarenaService = mock(VeilarbarenaService::class.java)
    var utrullingService = mock(UtrullingService::class.java)
    var abacClient = mock(AbacClient::class.java)
    var serviceUserCredentials = mock(Credentials::class.java)
    var poaoTilgangClient = org.mockito.kotlin.mock<PoaoTilgangClient>()
    var unleashService = mock(DefaultUnleash::class.java)
    var authService =
        AuthService(aktorOppslagClient, pep, veilarbarenaService, abacClient, serviceUserCredentials, authContextHolder, utrullingService, poaoTilgangClient, unleashService)

    @BeforeEach
    fun setup() {
        `when`(aktorOppslagClient.hentAktorId(TestData.TEST_FNR)).thenReturn(AktorId.of(TestData.TEST_AKTOR_ID))
        `when`(aktorOppslagClient.hentFnr(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(TestData.TEST_FNR)
        `when`(veilarbarenaService.hentOppfolgingsenhet(TestData.TEST_FNR)).thenReturn(Optional.of(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID)))
    }

    @Test
    fun sjekkTilgangTilBruker__gir_tilgang_for_intern_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Permit))
        withContext(UserRole.INTERN) { authService.sjekkVeilederTilgangTilBruker(TestData.TEST_FNR) }
    }

    @Test
    fun sjekkTilgangTilBruker__kaster_exception_for_andre_enn_ekstern_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        UserRole.values().filter { userRole: UserRole -> userRole != UserRole.INTERN }.forEach { userRole: UserRole ->
                withContext(userRole) {
                    assertThrowsWithMessage<ResponseStatusException>("""403 FORBIDDEN "Ikke intern bruker"""") {
                        authService.sjekkVeilederTilgangTilBruker(TestData.TEST_FNR)
                    }
                }
            }
        }

    @Test
    fun sjekkTilgangTilBruker__kaster_exception_ved_manglende_tilgang_til_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(false)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Deny("", "")))
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>("403 FORBIDDEN") {
                authService.sjekkVeilederTilgangTilBruker(TestData.TEST_FNR)
            }
        }
    }

    @Test
    fun sjekkTilgangTilBruker__skal_bruke_poao_tilgang_hvis_toggle_er_pa() {
        `when`(
            pep.harVeilederTilgangTilPerson(any(), any(), any())
        ).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Permit))
        withContext(UserRole.INTERN) {
                authService.sjekkVeilederTilgangTilBruker(TestData.TEST_FNR)
        }
        org.mockito.kotlin.verify(poaoTilgangClient, times(1)).evaluatePolicy(org.mockito.kotlin.any())
    }

    //@Test
    fun sjekkTilgangTilBruker__skal_kaste_exception_hvis_poao_tilgang_gir_decision_deny() {
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Deny("", "")))
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>("403 FORBIDDEN") {
                authService.sjekkVeilederTilgangTilBruker(TestData.TEST_FNR)
            }
        }
        org.mockito.kotlin.verify(poaoTilgangClient, times(1)).evaluatePolicy(org.mockito.kotlin.any())
    }

    @Test
    fun sjekkTilgangTilEnhet__skal_bruke_poao_tilgang_hvis_toggle_er_pa() {
        `when`(
            pep.harVeilederTilgangTilEnhet(any(), any())
        ).thenReturn(true)
        `when`(
            pep.harVeilederTilgangTilPerson(any(), any(), any())
        ).thenReturn(true)
        `when`(utrullingService.erUtrullet(any())).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Permit))
        withContext(UserRole.INTERN) {
            authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR)
        }
        org.mockito.kotlin.verify(poaoTilgangClient, times(1))
            .evaluatePolicy(org.mockito.kotlin.any<NavAnsattTilgangTilNavEnhetPolicyInput>())
    }


    @Test
    fun sjekkTilgangTilBrukerOgEnhet__sjekkTilgangTilBruker__gir_tilgang_for_intern_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Permit))
        `when`(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true)
        `when`(utrullingService.erUtrullet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(true)
        withContext(UserRole.INTERN) { authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR) }
    }

    @Test
    fun sjekkTilgangTilBrukerOgEnhet__kaster_exception_for_andre_enn_ekstern_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        `when`(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true)
        `when`(utrullingService.erUtrullet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(true)
        Arrays.stream(UserRole.values()).filter { userRole: UserRole -> userRole != UserRole.INTERN }
            .forEach { userRole: UserRole ->
                withContext(userRole) {
                    assertThrowsWithMessage<ResponseStatusException>("""403 FORBIDDEN "Ikke intern bruker"""") {
                        authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR)
                    }
                }
            }
    }

    @Test
    fun sjekkTilgangTilBrukerOgEnhet__kaster_exception_ved_manglende_tilgang_til_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(false)
        `when`(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Deny("", "")))
        `when`(utrullingService.erUtrullet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(true)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>("403 FORBIDDEN") {
                authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR)
            }
        }
    }

    @Test
    fun sjekkTilgangTilBrukerOgEnhet__kaster_exception_ved_manglende_tilgang_til_enhet() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        `when`(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(false)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Deny("", "")))
        `when`(utrullingService.erUtrullet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(true)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>("403 FORBIDDEN") {
                authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR)
            }
        }
    }

    @Test
    fun sjekkTilgangTilBrukerOgEnhet__kaster_exception_dersom_enhet_ikke_er_utrullet() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
        whenever(
            poaoTilgangClient.evaluatePolicy(org.mockito.kotlin.any())
        ).thenReturn(ApiResult.success(Decision.Permit))
        `when`(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true)
        `when`(utrullingService.erUtrullet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(false)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>(
                """403 FORBIDDEN "Vedtaksstøtte er ikke utrullet for enheten""""
            ) {
                authService.sjekkTilgangTilBrukerOgEnhet(TestData.TEST_FNR)
            }
        }
    }

    @Test
    fun lagSjekkTilgangRequest__skal_lage_riktig_request() {
        val request = authService.lagSjekkTilgangRequest("srvtest", "Z1234", Arrays.asList("11111111111", "2222222222"))
        val requestJson = XacmlMapper.mapRequestToEntity(request)
        val expectedRequestJson = readTestResourceFile("testdata/xacmlrequest-abac-tilgang.json")
        assertEquals(expectedRequestJson, requestJson)
    }

    @Test
    fun mapBrukerTilgangRespons__skal_mappe_riktig() {
        val responseJson = readTestResourceFile("testdata/xacmlresponse-abac-tilgang.json")
        val response = XacmlMapper.mapRawResponse(responseJson)
        val tilgangTilBrukere = authService.mapBrukerTilgangRespons(response)
        assertTrue(tilgangTilBrukere.getOrDefault("11111111111", false))
        assertFalse(tilgangTilBrukere.getOrDefault("2222222222", false))
    }

    @Test
    fun `erSystemBrukerMedSystemTilSystemTilgang er true for system med rolle access_as_application`() {
        authContextHolder.withContext(
            systemMedRoller("access_as_application")
        ) {
            assertTrue(authService.harSystemTilSystemTilgang())
        }
    }

    @Test
    fun `erSystemBrukerMedSystemTilSystemTilgang er false for system uten rolle access_as_application`() {
        authContextHolder.withContext(
            systemMedRoller("access_as_foo")
        ) {
            assertFalse(authService.harSystemTilSystemTilgang())
        }
    }

    @Test
    fun `harSystemTilSystemTilgangMedEkstraRolle er true for system med rolle access_as_application og ekstra rolle`() {
        authContextHolder.withContext(
            systemMedRoller("access_as_application", "ekstra_rolle")
        ) {
            assertTrue(authService.harSystemTilSystemTilgangMedEkstraRolle("ekstra_rolle"))
        }
    }

    @Test
    fun `harSystemTilSystemTilgangMedEkstraRolle er false for system med rolle access_as_application og uten ekstra rolle`() {
        authContextHolder.withContext(
            systemMedRoller("access_as_application")
        ) {
            assertFalse(authService.harSystemTilSystemTilgangMedEkstraRolle("ekstra_rolle"))
        }
    }

    @Test
    fun `harSystemTilSystemTilgangMedEkstraRolle er false for system uten rolle access_as_application og ekstra rolle`() {
        authContextHolder.withContext(
            systemMedRoller("ekstra_rolle")
        ) {
            assertFalse(authService.harSystemTilSystemTilgangMedEkstraRolle("ekstra_rolle"))
        }
    }

    private fun withContext(userRole: UserRole, runnable: UnsafeRunnable) {
        authContextHolder.withContext(createAuthContext(userRole, mapOf("sub" to TestData.TEST_VEILEDER_IDENT, "oid" to UUID.randomUUID().toString())), runnable)
    }

    private fun systemMedRoller(vararg roller: String): AuthContext {
        return createAuthContext(
            UserRole.SYSTEM,
            mapOf(
                Pair("sub", "123"),
                Pair("roles", roller.toList())
            )
        )
    }
}
