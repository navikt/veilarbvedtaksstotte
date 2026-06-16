package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiodeV3;
import no.nav.veilarbvedtaksstotte.domain.kafka.KontorDto;
import no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    private static final String TEST_TOPIC = "test-topic";

    private final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final Norg2Client norg2Client = mock(Norg2Client.class);

    private final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClientImpl.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository = mock(SisteOppfolgingPeriodeRepository.class);

    private final BrukerIdenterService brukerIdenterService = mock(BrukerIdenterService.class);

    private final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);

    private final KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
            vedtaksstotteRepository,
            beslutteroversiktRepository,
            sisteOppfolgingPeriodeRepository,
            brukerIdenterService,
            kafkaProducerService
    );

    @Test
    public void skal_sette_gjeldende_til_historisk_for_v3_melding_hvis_fattet_foer_oppfolging_avsluttet() {
        LocalDateTime nowMinus10Days = LocalDateTime.now().minusDays(10);
        ZonedDateTime sluttTidspunkt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
        when(vedtaksstotteRepository.hentGjeldendeVedtak(anyString())).thenReturn(new Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(nowMinus10Days).setAktorId(TEST_AKTOR_ID));

        kafkaConsumerService.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(new KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.OPPFOLGING_AVSLUTTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault()),
                sluttTidspunkt,
                null,
                ZonedDateTime.now()
            ))
        );

        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(anyLong());
    }

    @Test
    public void skal_ikke_sette_gjeldende_til_historisk_hvis_fattet_etter_oppfolging_avsluttet() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowMinus10Days = now.minusDays(10);
        LocalDateTime nowMinus20Days = now.minusDays(20);
        ZonedDateTime sluttTidspunkt = ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault());
        when(vedtaksstotteRepository.hentGjeldendeVedtak(anyString())).thenReturn(new Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(now));

        kafkaConsumerService.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(new KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.OPPFOLGING_AVSLUTTET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.of(nowMinus20Days, ZoneId.systemDefault()),
                sluttTidspunkt,
                null,
                ZonedDateTime.now()
            ))
        );

        verify(vedtaksstotteRepository, never()).settGjeldendeVedtakTilHistorisk(anyLong());
    }

    @Test
    public void skal_ikke_oppdatere_enhet_hvis_kontor_er_uendret() {
        String enhet = "4562";

        when(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(new Vedtak().setOppfolgingsenhetId(enhet));

        kafkaConsumerService.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(new KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                new KontorDto("TEST", enhet),
                ZonedDateTime.now()
            )));

        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString());
        verify(beslutteroversiktRepository, never()).oppdaterBrukerEnhet(anyLong(), anyString(), anyString());
    }

    @Test
    public void skal_oppdatere_enhet_hvis_kontor_er_endret() {
        String nyEnhet = "4562";

        when(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(new Vedtak().setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID));

        kafkaConsumerService.behandleSisteOppfolgingsperiodeV3(
            lagV3Record(new KafkaSisteOppfolgingsperiodeV3(
                UUID.randomUUID(),
                SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
                TEST_AKTOR_ID,
                TEST_FNR.get(),
                ZonedDateTime.now().minusDays(1),
                null,
                new KontorDto("TEST", nyEnhet),
                ZonedDateTime.now()
            )));

        verify(vedtaksstotteRepository, times(1)).oppdaterUtkastEnhet(anyLong(), eq(nyEnhet));
        verify(beslutteroversiktRepository, times(1)).oppdaterBrukerEnhet(anyLong(), eq(nyEnhet), eq("TEST"));
    }

    private ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3> lagV3Record(KafkaSisteOppfolgingsperiodeV3 melding) {
        return new ConsumerRecord<>(TEST_TOPIC, 0, 0, 0L, melding);
    }

}
