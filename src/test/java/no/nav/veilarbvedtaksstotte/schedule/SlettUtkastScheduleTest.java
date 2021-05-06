package no.nav.veilarbvedtaksstotte.schedule;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.UtkastetVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SlettUtkastScheduleTest {

    private LeaderElectionClient leaderElectionClient = mock(LeaderElectionClient.class);

    private VeilarboppfolgingClient veilarboppfolgingClient = mock(VeilarboppfolgingClient.class);

    private AktorregisterClient aktorregisterClient = mock(AktorregisterClient.class);

    private VedtakService vedtakService = mock(VedtakService.class);

    private VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private SlettUtkastSchedule slettUtkastSchedule = new SlettUtkastSchedule(
            leaderElectionClient, veilarboppfolgingClient,
            aktorregisterClient, vedtakService, vedtaksstotteRepository
    );

    @Test
    public void slettGamleUtkast__skal_slette_gammelt_utkast() {
        UtkastetVedtak gammeltUtkast = getGammelUtkastetVedtak();

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(LocalDateTime.now().minusDays(50));
        oppfolgingPeriode.setSluttDato(LocalDateTime.now().minusDays(30));

        when(vedtaksstotteRepository.hentUtkastEldreEnn(any())).thenReturn(List.of(gammeltUtkast));
        when(vedtaksstotteRepository.hentGjeldendeVedtak(any())).thenReturn(null);
        when(aktorregisterClient.hentFnr(any(AktorId.class))).thenReturn(Fnr.of("test"));
        when(veilarboppfolgingClient.hentOppfolgingsperioder(any())).thenReturn(List.of(oppfolgingPeriode));

        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, times(1)).slettUtkast(any());
    }

    @Test
    public void slettGamleUtkast__skal_ikke_slette_utkast_innenfor_28_dager() {
        UtkastetVedtak gammeltUtkast = getGammelUtkastetVedtak();

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(LocalDateTime.now().minusDays(20));
        oppfolgingPeriode.setSluttDato(LocalDateTime.now().minusDays(18));

        when(vedtaksstotteRepository.hentUtkastEldreEnn(any())).thenReturn(List.of(gammeltUtkast));
        when(vedtaksstotteRepository.hentGjeldendeVedtak(any())).thenReturn(null);
        when(aktorregisterClient.hentFnr(any(AktorId.class))).thenReturn(Fnr.of("test"));
        when(veilarboppfolgingClient.hentOppfolgingsperioder(any())).thenReturn(List.of(oppfolgingPeriode));

        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, never()).slettUtkast(any());
    }

    private UtkastetVedtak getGammelUtkastetVedtak() {
        return UtkastetVedtak.builder()
                .aktorId("22222222")
                .vedtakStatus(VedtakStatus.UTKAST)
                .sistOppdatert(LocalDateTime.now().minusDays(30))
                .build();
    }
}
