package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode;
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

import static no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImplTest.veilarbarenaClient;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaConsumerServiceTest {

    private final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final Norg2Client norg2Client = mock(Norg2Client.class);

    private final Siste14aVedtakService siste14aVedtakService = mock(Siste14aVedtakService.class);

    private final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClientImpl.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository = mock(SisteOppfolgingPeriodeRepository.class);

    private final BrukerIdenterService brukerIdenterService = mock(BrukerIdenterService.class);

    private final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);

    private final KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
            siste14aVedtakService,
            vedtaksstotteRepository,
            beslutteroversiktRepository,
            sisteOppfolgingPeriodeRepository,
            norg2Client,
            aktorOppslagClient,
            veilarbarenaClient,
            brukerIdenterService,
            kafkaProducerService
    );

    @Test
    public void skal_sette_gjeldende_til_historisk_hvis_fattet_foer_oppfolging_avsluttet() {
        LocalDateTime nowMinus10Days = LocalDateTime.now().minusDays(10);
        ZonedDateTime oppfolgingAvsluttetDato = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
        when(vedtaksstotteRepository.hentGjeldendeVedtak(anyString())).thenReturn(new Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(nowMinus10Days).setAktorId(TEST_AKTOR_ID));

        kafkaConsumerService.behandleSisteOppfolgingsperiode(
                new ConsumerRecord<>("", 0, 0, TEST_AKTOR_ID, new KafkaSisteOppfolgingsperiode(UUID.randomUUID(), TEST_AKTOR_ID, ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault()), oppfolgingAvsluttetDato))
        );

        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(anyLong());
    }

    @Test
    public void skal_ikke_sette_gjeldende_til_historisk_hvis_fattet_etter_oppfolging_avsluttet() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowMinus10Days = now.minusDays(10);
        LocalDateTime nowMinus20Days = now.minusDays(20);
        ZonedDateTime oppfolgingAvsluttetDato = ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault());
        when(vedtaksstotteRepository.hentGjeldendeVedtak(anyString())).thenReturn(new Vedtak().setId(1234L).setGjeldende(true).setVedtakFattet(now));

        kafkaConsumerService.behandleSisteOppfolgingsperiode(
                new ConsumerRecord<>("", 0, 0, "", new KafkaSisteOppfolgingsperiode(UUID.randomUUID(), TEST_AKTOR_ID, ZonedDateTime.of(nowMinus20Days, ZoneId.systemDefault()), oppfolgingAvsluttetDato))
        );

        verify(vedtaksstotteRepository, never()).settGjeldendeVedtakTilHistorisk(anyLong());
    }

    @Test
    public void skal_ikke_oppdatere_enhet_hvis_enhet_er_lik_for_endring_pa_oppfolgingsbruker() {
        String enhet = "4562";

        when(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(new Vedtak().setOppfolgingsenhetId(enhet));
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));

        kafkaConsumerService.flyttingAvOppfolgingsbrukerTilNyEnhet(
                new ConsumerRecord<>("", 0, 0, "", new KafkaOppfolgingsbrukerEndringV2(TEST_FNR, enhet)));

        verify(norg2Client, never()).hentEnhet(enhet);
        verify(vedtaksstotteRepository, never()).oppdaterUtkastEnhet(anyLong(), anyString());
        verify(beslutteroversiktRepository, never()).oppdaterBrukerEnhet(anyLong(), anyString(), anyString());
    }

    @Test
    public void skal_oppdatere_enhet_hvis_enhet_er_ulik_for_endring_pa_oppfolgingsbruker() {
        String nyEnhet = "4562";

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, nyEnhet);

        when(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(new Vedtak().setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID));
        when(norg2Client.hentEnhet(nyEnhet)).thenReturn(new Enhet().setNavn("TEST"));
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));

        kafkaConsumerService.flyttingAvOppfolgingsbrukerTilNyEnhet(
                new ConsumerRecord<>("", 0, 0, "", new KafkaOppfolgingsbrukerEndringV2(TEST_FNR, nyEnhet)));

        verify(norg2Client, times(1)).hentEnhet(nyEnhet);
        verify(vedtaksstotteRepository, times(1)).oppdaterUtkastEnhet(anyLong(), eq(nyEnhet));
        verify(beslutteroversiktRepository, times(1)).oppdaterBrukerEnhet(anyLong(), eq(nyEnhet), anyString());
    }

}
