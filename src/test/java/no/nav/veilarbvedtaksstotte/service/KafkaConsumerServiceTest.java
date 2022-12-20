package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static no.nav.veilarbvedtaksstotte.utils.DatabaseTest.jdbcTemplate;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    private final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final Norg2Client norg2Client = mock(Norg2Client.class);

    private final Siste14aVedtakService siste14aVedtakService = mock(Siste14aVedtakService.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
            siste14aVedtakService,
            vedtaksstotteRepository,
            beslutteroversiktRepository,
            norg2Client,
            aktorOppslagClient);

    @Test
    public void skal_behandle_endring_pa_avslutt_oppfolging() {
        String aktorId = randomNumeric(10);
        long vedtakID = 1234;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowMinus10Days = now.minusDays(10);
        ZonedDateTime time = ZonedDateTime.now();
        when(vedtaksstotteRepository.hentSisteVedtak(aktorId)).thenReturn(new Vedtak().setId(vedtakID).setAktorId(aktorId).setVedtakFattet(nowMinus10Days).setGjeldende(true));

        kafkaConsumerService.behandleEndringPaAvsluttOppfolging(
                new ConsumerRecord<>("", 0, 0, "", new KafkaAvsluttOppfolging(aktorId, time)));

        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(eq(vedtakID));
    }

    @Test
    public void skal_ikke_sette_gjeldende_til_historisk_hvis_fattet_etter_oppfolging_avsluttet() {
        //given at vi har et vedtak som står som gjeldende
        // vedtaket er fattet etter oppfølging avsluttet
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowMinus10Days = now.minusDays(10);
        ZonedDateTime oppfolgingAvsluttetDato = ZonedDateTime.of(nowMinus10Days, ZoneId.systemDefault());

        when(vedtaksstotteRepository.hentSisteVedtak(TEST_AKTOR_ID)).thenReturn(new Vedtak().setGjeldende(true).setVedtakFattet(LocalDateTime.now()));


        //when vi kaller behandleEndringPaAvsluttOppfolging
        // when vi mottar en kafkamelding om oppfølging avsluttet

        kafkaConsumerService.behandleEndringPaAvsluttOppfolging(
                new ConsumerRecord<>("", 0, 0, "", new KafkaAvsluttOppfolging(TEST_AKTOR_ID, oppfolgingAvsluttetDato))
        );


        //then skal vedtaket ikke settes til historisk
        verify(vedtaksstotteRepository, never()).settGjeldendeVedtakTilHistorisk(anyLong());

    }


    @Test
    public void skal_sette_gjeldende_til_historisk_hvis_fattet_foer_oppfolging_avsluttet() {
        //given at vi har et vedtak som står som gjeldende
        // vedtaket er fattet etter oppfølging avsluttet
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowMinus10Days = now.minusDays(10);
        ZonedDateTime oppfolgingAvsluttetDato = ZonedDateTime.of(now, ZoneId.systemDefault());

        when(vedtaksstotteRepository.hentSisteVedtak(TEST_AKTOR_ID)).thenReturn(new Vedtak().setGjeldende(true).setVedtakFattet(nowMinus10Days));


        //when vi kaller behandleEndringPaAvsluttOppfolging
        // when vi mottar en kafkamelding om oppfølging avsluttet

        kafkaConsumerService.behandleEndringPaAvsluttOppfolging(
                new ConsumerRecord<>("", 0, 0, "", new KafkaAvsluttOppfolging(TEST_AKTOR_ID, oppfolgingAvsluttetDato))
        );


        //then skal vedtaket ikke settes til historisk
        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(anyLong());

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
