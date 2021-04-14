package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Test;

import java.time.ZonedDateTime;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    private VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private Norg2Client norg2Client = mock(Norg2Client.class);

    private KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(vedtaksstotteRepository, beslutteroversiktRepository, norg2Client);

    @Test
    public void skal_behandle_endring_pa_avslutt_oppfolging() {
        String aktorId = "1234";

        kafkaConsumerService.behandleEndringPaAvsluttOppfolging(new KafkaAvsluttOppfolging(aktorId, ZonedDateTime.now()));

        verify(vedtaksstotteRepository, times(1)).settGjeldendeVedtakTilHistorisk(eq(aktorId));
    }

    @Test
    public void skal_ikke_oppdatere_enhet_hvis_enhet_er_lik_for_endring_pa_oppfolgingsbruker() {
        String enhet = "4562";

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, enhet);

        when(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID)).thenReturn(new Vedtak().setOppfolgingsenhetId(enhet));

        kafkaConsumerService.behandleEndringPaOppfolgingsbruker(new KafkaOppfolgingsbrukerEndring(TEST_AKTOR_ID, enhet));

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

        kafkaConsumerService.behandleEndringPaOppfolgingsbruker(new KafkaOppfolgingsbrukerEndring(TEST_AKTOR_ID, nyEnhet));

        verify(norg2Client, times(1)).hentEnhet(nyEnhet);
        verify(vedtaksstotteRepository, times(1)).oppdaterUtkastEnhet(anyLong(), eq(nyEnhet));
        verify(beslutteroversiktRepository, times(1)).oppdaterBrukerEnhet(anyLong(), eq(nyEnhet), anyString());
    }

}
