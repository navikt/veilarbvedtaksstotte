package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.test.auth.AuthTestUtils.createAuthContext
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils.assertThrowsWithMessage
import no.nav.veilarbvedtaksstotte.utils.VIS_VEDTAKSLOSNING_14A
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.web.server.ResponseStatusException
import java.util.*

class UtrullingServiceTest {
    var authContextHolder = AuthContextHolderThreadLocal.instance()
    var aktorOppslagClient = mock(AktorOppslagClient::class.java)
    var veilarbarenaService = mock(VeilarbarenaService::class.java)
    var utrullingRepository = mock(UtrullingRepository::class.java)
    var veilarbarenaClient = mock(VeilarbarenaClient::class.java)
    var veilarbveilederClient = mock(VeilarbveilederClient::class.java)
    var norg2Client = mock(Norg2Client::class.java)
    var vedtaksstotteRepository = mock(VedtaksstotteRepository::class.java)
    var unleashClient = mock(DefaultUnleash::class.java)

    var utrullingService = UtrullingService(
        utrullingRepository,
        veilarbarenaClient,
        veilarbveilederClient,
        norg2Client,
        vedtaksstotteRepository,
        aktorOppslagClient,
        unleashClient
    )
    val fnr = Fnr.of("01010111111")
    val vedtakId: Long = 1

    @BeforeEach
    fun setup() {
        `when`(aktorOppslagClient.hentAktorId(TestData.TEST_FNR)).thenReturn(AktorId.of(TestData.TEST_AKTOR_ID))
        `when`(aktorOppslagClient.hentFnr(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(TestData.TEST_FNR)
        `when`(veilarbarenaService.hentOppfolgingsenhet(TestData.TEST_FNR)).thenReturn(Optional.of(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID)))
    }

    @Test
    fun sjekkAtBrukerTilhorerUtrulletKontor__utløser_unntak_dersom_enhet_ikke_er_utrullet__fnr() {
        `when`(utrullingRepository.erUtrullet(any())).thenReturn(false)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>(
                """403 FORBIDDEN "Vedtaksstøtte er ikke utrullet for veileder""""
            ) {
                utrullingService.sjekkOmVeilederSkalHaTilgangTilNyLosning(fnr)
            }
        }
    }

    @Test
    fun sjekkOmVeilederSkalHaTilgangTilNyLosning__får_feilmelding_når_vedtak_mangler() {
        `when`(utrullingRepository.erUtrullet(any())).thenReturn(false)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>(
                """404 NOT_FOUND "Fant ikke vedtak med vedtakId $vedtakId""""
            ) {
                utrullingService.sjekkOmVeilederSkalHaTilgangTilNyLosning(vedtakId)
            }
        }
    }

    @Test
    fun sjekkAtBrukerTilhorerUtrulletKontor__utløser_unntak_dersom_enhet_ikke_er_utrullet__vedtakId() {
        val vedtak = Vedtak()
        `when`(utrullingRepository.erUtrullet(any())).thenReturn(false)
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        withContext(UserRole.INTERN) {
            assertThrowsWithMessage<ResponseStatusException>(
                """403 FORBIDDEN "Vedtaksstøtte er ikke utrullet for veileder""""
            ) {
                utrullingService.sjekkOmVeilederSkalHaTilgangTilNyLosning(fnr)
            }
        }
    }

    @Test
    fun sjekkAtBrukerTilhorerUtrulletKontor__ikke_utløser_unntak_dersom_unleashtoggle_er_pa() {
        val vedtak = Vedtak()
        `when`(utrullingRepository.erUtrullet(any())).thenReturn(false)
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        `when`(unleashClient.isEnabled(VIS_VEDTAKSLOSNING_14A)).thenReturn(true)
        withContext(UserRole.INTERN) {
            assertDoesNotThrow {
                utrullingService.sjekkOmVeilederSkalHaTilgangTilNyLosning(fnr)
            }
        }
    }

    private fun withContext(userRole: UserRole, runnable: UnsafeRunnable) {
        authContextHolder.withContext(
            createAuthContext(
                userRole,
                mapOf("sub" to TestData.TEST_VEILEDER_IDENT, "oid" to UUID.randomUUID().toString())
            ), runnable
        )
    }
}