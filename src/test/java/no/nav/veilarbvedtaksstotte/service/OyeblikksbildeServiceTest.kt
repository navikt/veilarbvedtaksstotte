package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogResponse
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClient
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AggregertPeriode
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Bruker
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.BrukerType
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Egenvurdering
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Metadata
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.PeriodeStartet
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.ProfilertTil
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingData
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeArbeidssokerRegistretDto
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeCvDto
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeEgenvurderingDto
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*

internal class OyeblikksbildeServiceTest {

    private val authService = mock(AuthService::class.java)
    private val oyeblikksbildeRepository = mock(OyeblikksbildeRepository::class.java)
    private val vedtaksstotteRepository = mock(VedtaksstotteRepository::class.java)
    private val veilarbpersonClient = mock(VeilarbpersonClient::class.java)
    private val arbeidssoekerregisteretApiOppslagV2Client = mock(ArbeidssoekerregisteretApiOppslagV2Client::class.java)
    private val egenvurderingDialogTjenesteClient = mock(EgenvurderingDialogTjenesteClient::class.java)
    
    private lateinit var oyeblikksbildeService: OyeblikksbildeService

    @BeforeEach
    fun setup() {
        reset(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, 
              arbeidssoekerregisteretApiOppslagV2Client, egenvurderingDialogTjenesteClient)
        
        oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            arbeidssoekerregisteretApiOppslagV2Client,
            egenvurderingDialogTjenesteClient
        )
    }

    @Test
    fun lagreOyeblikksbilderPaaNynorsk() {
        val fnr = "12345678910"
        val egenvurderingTekst = "Svara dine om behov for rettleiing"
        val arbeissokerregisteretTekst = "Det du fortalde oss da du vart registrert som arbeidssøkar"
        val kilder = listOf(
            KildeEntity(egenvurderingTekst, UUID.randomUUID()),
            KildeEntity(arbeissokerregisteretTekst, UUID.randomUUID())
        )
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, 12344, kilder)
        
        verify(oyeblikksbildeRepository, times(1))
            .upsertArbeidssokerRegistretOyeblikksbilde(12344, null)
    }

    @Test
    fun lagreOyeblikksbilde_medCVKilde_skalLagreCVOyeblikksbilde() {
        val fnr = "12345678910"
        val vedtakId = 123L
        val cvInnhold = mock(CvInnhold::class.java)
        val cvDto = CvDto.CVMedInnhold(cvInnhold)
        val kilder = listOf(KildeEntity(VedtakOpplysningKilder.CV.desc, UUID.randomUUID()))
        
        `when`(veilarbpersonClient.hentCVOgJobbprofil(fnr)).thenReturn(cvDto)
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, kilder)
        
        verify(veilarbpersonClient).hentCVOgJobbprofil(fnr)
        verify(oyeblikksbildeRepository).upsertCVOyeblikksbilde(vedtakId, cvDto)
    }

    @Test
    fun lagreOyeblikksbilde_medArbeidssokerregisteretKilde_skalLagre() {
        val fnr = "12345678910"
        val vedtakId = 789L
        val opplysninger = mock(OpplysningerOmArbeidssoekerMedProfilering::class.java)
        val kilder = listOf(KildeEntity(VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.desc, UUID.randomUUID()))
        
        `when`(veilarbpersonClient.hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr.of(fnr)))
            .thenReturn(opplysninger)
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, kilder)
        
        verify(veilarbpersonClient).hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr.of(fnr))
        verify(oyeblikksbildeRepository).upsertArbeidssokerRegistretOyeblikksbilde(vedtakId, opplysninger)
    }

    @Test
    fun lagreOyeblikksbilde_medEgenvurderingKilde_og_dialogId_skalLagre() {
        val fnr = "12345678910"
        val vedtakId = 999L
        val periodeId = UUID.randomUUID()
        val dialogId = 12345L
        val kilder = listOf(KildeEntity(VedtakOpplysningKilder.EGENVURDERING.desc, UUID.randomUUID()))
        
        val metadata = Metadata(
            tidspunkt = LocalDateTime.now(),
            utfoertAv = Bruker(type = BrukerType.SLUTTBRUKER, id = fnr),
            kilde = "test",
            aarsak = "test"
        )
        val egenvurdering = Egenvurdering(
            type = Egenvurdering.Type.EGENVURDERING_V1,
            id = UUID.randomUUID(),
            profileringId = UUID.randomUUID(),
            sendtInnAv = metadata,
            profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            egenvurdering = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = LocalDateTime.now()
        )
        val aggregertPeriode = AggregertPeriode(
            id = periodeId,
            identitetsnummer = fnr,
            startet = PeriodeStartet(
                type = PeriodeStartet.Type.PERIODE_STARTET_V1,
                sendtInnAv = metadata,
                tidspunkt = LocalDateTime.now()
            ),
            egenvurdering = egenvurdering
        )
        
        `when`(arbeidssoekerregisteretApiOppslagV2Client.hentEgenvurdering(NorskIdent.of(fnr)))
            .thenReturn(aggregertPeriode)
        `when`(egenvurderingDialogTjenesteClient.hentDialogId(periodeId))
            .thenReturn(EgenvurderingDialogResponse(dialogId))
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, kilder)
        
        verify(arbeidssoekerregisteretApiOppslagV2Client).hentEgenvurdering(NorskIdent.of(fnr))
        verify(egenvurderingDialogTjenesteClient).hentDialogId(periodeId)
        verify(oyeblikksbildeRepository).upsertEgenvurderingV2Oyeblikksbilde(eq(vedtakId), any())
    }

    @Test
    fun lagreOyeblikksbilde_medEgenvurderingKilde_men_utenDialogId_skalLagre() {
        val fnr = "12345678910"
        val vedtakId = 999L
        val periodeId = UUID.randomUUID()
        val kilder = listOf(KildeEntity(VedtakOpplysningKilder.EGENVURDERING.desc, UUID.randomUUID()))

        val metadata = Metadata(
            tidspunkt = LocalDateTime.now(),
            utfoertAv = Bruker(type = BrukerType.SLUTTBRUKER, id = fnr),
            kilde = "test",
            aarsak = "test"
        )
        val egenvurdering = Egenvurdering(
            type = Egenvurdering.Type.EGENVURDERING_V1,
            id = UUID.randomUUID(),
            profileringId = UUID.randomUUID(),
            sendtInnAv = metadata,
            profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            egenvurdering = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = LocalDateTime.now()
        )
        val aggregertPeriode = AggregertPeriode(
            id = periodeId,
            identitetsnummer = fnr,
            startet = PeriodeStartet(
                type = PeriodeStartet.Type.PERIODE_STARTET_V1,
                sendtInnAv = metadata,
                tidspunkt = LocalDateTime.now()
            ),
            egenvurdering = egenvurdering
        )

        `when`(arbeidssoekerregisteretApiOppslagV2Client.hentEgenvurdering(NorskIdent.of(fnr)))
            .thenReturn(aggregertPeriode)
        `when`(egenvurderingDialogTjenesteClient.hentDialogId(periodeId))
            .thenReturn(null)

        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, kilder)

        verify(arbeidssoekerregisteretApiOppslagV2Client).hentEgenvurdering(NorskIdent.of(fnr))
        verify(egenvurderingDialogTjenesteClient).hentDialogId(periodeId)
        verify(oyeblikksbildeRepository).upsertEgenvurderingV2Oyeblikksbilde(eq(vedtakId), any())
    }

    @Test
    fun lagreOyeblikksbilde_medTommeKilder_skalIkkeLagreNoe() {
        val fnr = "12345678910"
        val vedtakId = 111L
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, emptyList())
        
        verify(oyeblikksbildeRepository, never()).upsertCVOyeblikksbilde(anyLong(), any())
        verify(oyeblikksbildeRepository, never()).upsertArbeidssokerRegistretOyeblikksbilde(anyLong(), any())
        verify(oyeblikksbildeRepository, never()).upsertEgenvurderingV2Oyeblikksbilde(anyLong(), any())
    }

    @Test
    fun lagreOyeblikksbilde_medNullKilder_skalIkkeLagreNoe() {
        val fnr = "12345678910"
        val vedtakId = 222L
        
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId, null)
        
        verify(oyeblikksbildeRepository, never()).upsertCVOyeblikksbilde(anyLong(), any())
        verify(oyeblikksbildeRepository, never()).upsertArbeidssokerRegistretOyeblikksbilde(anyLong(), any())
        verify(oyeblikksbildeRepository, never()).upsertEgenvurderingV2Oyeblikksbilde(anyLong(), any())
    }

    @Test
    fun hentCVOyeblikksbildeForVedtak_finnesOyeblikksbilde_skalReturnereOyeblikksbilde() {
        val vedtakId = 333L
        val aktorId = "1234567890"
        val vedtak = mock(Vedtak::class.java)
        val cvInnhold = mock(CvInnhold::class.java)
        val oyeblikksbilde = OyeblikksbildeCvDto(cvInnhold, true)
        
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        `when`(vedtak.aktorId).thenReturn(aktorId)
        `when`(oyeblikksbildeRepository.hentCVOyeblikksbildeForVedtak(vedtakId))
            .thenReturn(Optional.of(oyeblikksbilde))
        
        val result = oyeblikksbildeService.hentCVOyeblikksbildeForVedtak(vedtakId)
        
        assertEquals(oyeblikksbilde, result)
        verify(authService).sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(aktorId))
    }

    @Test
    fun hentCVOyeblikksbildeForVedtak_finnesIkkeOyeblikksbilde_skalReturnereTomtOyeblikksbilde() {
        val vedtakId = 444L
        val aktorId = "1234567890"
        val vedtak = mock(Vedtak::class.java)
        
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        `when`(vedtak.aktorId).thenReturn(aktorId)
        `when`(oyeblikksbildeRepository.hentCVOyeblikksbildeForVedtak(vedtakId))
            .thenReturn(Optional.empty())
        
        val result = oyeblikksbildeService.hentCVOyeblikksbildeForVedtak(vedtakId)
        
        assertNotNull(result)
        assertNull(result.data)
        assertFalse(result.journalfort)
    }

    @Test
    fun hentArbeidssokerRegistretOyeblikksbildeForVedtak_finnesOyeblikksbilde_skalReturnereOyeblikksbilde() {
        val vedtakId = 555L
        val aktorId = "1234567890"
        val vedtak = mock(Vedtak::class.java)
        val opplysninger = mock(OpplysningerOmArbeidssoekerMedProfilering::class.java)
        val oyeblikksbilde = OyeblikksbildeArbeidssokerRegistretDto(opplysninger, true)
        
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        `when`(vedtak.aktorId).thenReturn(aktorId)
        `when`(oyeblikksbildeRepository.hentArbeidssokerRegistretOyeblikksbildeForVedtak(vedtakId))
            .thenReturn(Optional.of(oyeblikksbilde))
        
        val result = oyeblikksbildeService.hentArbeidssokerRegistretOyeblikksbildeForVedtak(vedtakId)
        
        assertEquals(oyeblikksbilde, result)
        verify(authService).sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(aktorId))
    }

    @Test
    fun hentEgenvurderingOyeblikksbildeForVedtak_finnesOyeblikksbilde_skalReturnereOyeblikksbilde() {
        val vedtakId = 666L
        val aktorId = "1234567890"
        val vedtak = mock(Vedtak::class.java)
        val egenvurderingData = mock(EgenvurderingData::class.java)
        val oyeblikksbilde = OyeblikksbildeEgenvurderingDto(egenvurderingData, true, OyeblikksbildeType.EGENVURDERING)
        
        `when`(vedtaksstotteRepository.hentVedtak(vedtakId)).thenReturn(vedtak)
        `when`(vedtak.aktorId).thenReturn(aktorId)
        `when`(oyeblikksbildeRepository.hentEgenvurderingOyeblikksbildeForVedtak(vedtakId))
            .thenReturn(Optional.of(oyeblikksbilde))
        
        val result = oyeblikksbildeService.hentEgenvurderingOyeblikksbildeForVedtak(vedtakId)
        
        assertEquals(oyeblikksbilde, result)
        verify(authService).sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(aktorId))
    }

    @Test
    fun slettOyeblikksbilde_skalKalleRepository() {
        val vedtakId = 777L
        
        oyeblikksbildeService.slettOyeblikksbilde(vedtakId)
        
        verify(oyeblikksbildeRepository).slettOyeblikksbilder(vedtakId)
    }
}
