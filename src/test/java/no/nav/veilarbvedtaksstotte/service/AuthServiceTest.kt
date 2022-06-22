package no.nav.veilarbvedtaksstotte.service

import no.nav.common.abac.Pep
import no.nav.common.abac.XacmlMapper
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils.assertThrowsWithMessage
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.util.*

class AuthServiceTest {
    var authContextHolder = AuthContextHolderThreadLocal.instance()
    var aktorOppslagClient = Mockito.mock(AktorOppslagClient::class.java)
    var pep = Mockito.mock(Pep::class.java)
    var veilarbarenaService = Mockito.mock(VeilarbarenaService::class.java)
    var utrullingService = Mockito.mock(UtrullingService::class.java)
    var authService =
        AuthService(aktorOppslagClient, pep, veilarbarenaService, null, null, authContextHolder, utrullingService, null, null)

    @Before
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
        withContext(UserRole.INTERN) { authService.sjekkTilgangTilBruker(TestData.TEST_FNR) }
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
                        authService.sjekkTilgangTilBruker(TestData.TEST_FNR)
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
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>("403 FORBIDDEN") {
                authService.sjekkTilgangTilBruker(TestData.TEST_FNR)
            }
        }
    }

    @Test
    fun sjekkTilgangTilBrukerOgEnhet__sjekkTilgangTilBruker__gir_tilgang_for_intern_bruker() {
        `when`(
            pep.harVeilederTilgangTilPerson(
                any(), any(), any()
            )
        ).thenReturn(true)
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
        val expectedRequestJson = readTestResourceFile("xacmlrequest-abac-tilgang.json")
        Assert.assertEquals(expectedRequestJson, requestJson)
    }

    @Test
    fun mapBrukerTilgangRespons__skal_mappe_riktig() {
        val responseJson = readTestResourceFile("xacmlresponse-abac-tilgang.json")
        val response = XacmlMapper.mapRawResponse(responseJson)
        val tilgangTilBrukere = authService.mapBrukerTilgangRespons(response)
        assertTrue(tilgangTilBrukere.getOrDefault("11111111111", false))
        assertFalse(tilgangTilBrukere.getOrDefault("2222222222", false))
    }

    private fun withContext(userRole: UserRole, runnable: UnsafeRunnable) {
        authContextHolder.withContext(AuthTestUtils.createAuthContext(userRole, TestData.TEST_VEILEDER_IDENT), runnable)
    }
}
