package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiodeV3
import no.nav.veilarbvedtaksstotte.domain.kafka.KontorDto
import no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class KafkaSisteOppfolgingsperiodeV3ConsumerTest {

    private val vedtaksstotteRepository: VedtaksstotteRepository = mock()
    private val beslutteroversiktRepository: BeslutteroversiktRepository = mock()
    private val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository = mock()
    private val kafkaProducerService: KafkaProducerService = mock()

    private val consumer = KafkaSisteOppfolgingsperiodeV3Consumer(
        vedtaksstotteRepository,
        beslutteroversiktRepository,
        sisteOppfolgingPeriodeRepository,
        kafkaProducerService
    )

    @Test
    fun `skal sette gjeldende til historisk hvis fattet for oppfolging avsluttet`() {
        val nowMinus10Days = LocalDateTime.now().minusDays(10)
        val sluttTidspunkt = ZonedDateTime.now()
        `when`(vedtaksstotteRepository.hentGjeldendeVedtak(anyString()))
            .thenReturn(Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(nowMinus10Days).setAktorId(TEST_AKTOR_ID))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.OPPFOLGING_AVSLUTTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault()),
                sluttTidspunkt,
                null,
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(anyLong())
    }

    @Test
    fun `skal ikke sette gjeldende til historisk hvis fattet etter oppfolging avsluttet`() {
        val now = LocalDateTime.now()
        val sluttTidspunkt = ZonedDateTime.of(now.minusDays(10), ZoneId.systemDefault())
        `when`(vedtaksstotteRepository.hentGjeldendeVedtak(anyString()))
            .thenReturn(Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(now))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.OPPFOLGING_AVSLUTTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.of(now.minusDays(20), ZoneId.systemDefault()),
                sluttTidspunkt,
                null,
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, never()).settGjeldendeVedtakTilHistorisk(anyLong())
    }

    @Test
    fun `skal ikke oppdatere enhet hvis kontor er uendret`() {
        val enhet = "4562"
        `when`(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(Vedtak().setOppfolgingsenhetId(enhet))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                KontorDto("TEST", enhet),
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString())
        verify(beslutteroversiktRepository, never()).oppdaterBrukerEnhet(anyLong(), anyString(), anyString())
    }

    @Test
    fun `skal oppdatere enhet hvis kontor er endret`() {
        val nyEnhet = "4562"
        `when`(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(Vedtak().setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                KontorDto("TEST", nyEnhet),
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, times(1)).oppdaterUtkastEnhet(anyLong(), eq(nyEnhet))
        verify(beslutteroversiktRepository, times(1)).oppdaterBrukerEnhet(anyLong(), eq(nyEnhet), eq("TEST"))
    }

    @Test
    fun `skal ikke oppdatere enhet hvis ingen utkast finnes`() {
        `when`(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(null)

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                KontorDto("TEST", "4562"),
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString())
    }

    @Test
    fun `skal ikke oppdatere enhet hvis kontor er null`() {
        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                null,
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, never()).hentUtkast(anyString())
        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString())
    }

    @Test
    fun `skal ikke oppdatere enhet hvis melding er utdatert`() {
        val lagretStart = ZonedDateTime.now()
        val gammelStart = lagretStart.minusDays(30)

        `when`(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(Vedtak().setOppfolgingsenhetId("1111"))
        `when`(sisteOppfolgingPeriodeRepository.hentSisteOppfolgingsperiode(AktorId.of(TEST_AKTOR_ID)))
            .thenReturn(SisteOppfolgingsperiode(UUID.randomUUID(), AktorId.of(TEST_AKTOR_ID), lagretStart, null))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                gammelStart,
                null,
                KontorDto("TEST", "4562"),
                ZonedDateTime.now()
            ))
        )

        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString())
    }

    @Test
    fun `skal upserte oppfolgingsperiode ved oppfolging startet`() {
        val periodeUuid = UUID.randomUUID()
        val startTidspunkt = ZonedDateTime.now()

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                periodeUuid,
                SisteEndringsType.OPPFOLGING_STARTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                startTidspunkt,
                null,
                KontorDto("TEST", "4562"),
                ZonedDateTime.now()
            ))
        )

        verify(sisteOppfolgingPeriodeRepository).upsertSisteOppfolgingPeriode(periodeUuid, TEST_AKTOR_ID, startTidspunkt, null)
    }

    @Test
    fun `skal ikke upserte oppfolgingsperiode hvis oppfolging startet er utdatert`() {
        val lagretStart = ZonedDateTime.now()
        val gammelStart = lagretStart.minusDays(30)

        `when`(sisteOppfolgingPeriodeRepository.hentSisteOppfolgingsperiode(AktorId.of(TEST_AKTOR_ID)))
            .thenReturn(SisteOppfolgingsperiode(UUID.randomUUID(), AktorId.of(TEST_AKTOR_ID), lagretStart, null))

        consumer.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.OPPFOLGING_STARTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                gammelStart,
                null,
                KontorDto("TEST", "4562"),
                ZonedDateTime.now()
            ))
        )

        verify(sisteOppfolgingPeriodeRepository).hentSisteOppfolgingsperiode(AktorId.of(TEST_AKTOR_ID))
        verifyNoMoreInteractions(sisteOppfolgingPeriodeRepository)
    }

    private fun lagV3Record(melding: KafkaSisteOppfolgingsperiodeV3): ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3> {
        return ConsumerRecord(TEST_TOPIC, 0, 0, 0L, melding)
    }

    companion object {
        private const val TEST_TOPIC = "test-topic"
    }
}
