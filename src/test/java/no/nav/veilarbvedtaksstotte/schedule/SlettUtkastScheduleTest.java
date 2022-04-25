package no.nav.veilarbvedtaksstotte.schedule;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

public class SlettUtkastScheduleTest {

    private LeaderElectionClient leaderElectionClient = mock(LeaderElectionClient.class);

    private VeilarboppfolgingClient veilarboppfolgingClient = mock(VeilarboppfolgingClient.class);

    private AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private VedtakService vedtakService = mock(VedtakService.class);

    private VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private SlettUtkastSchedule slettUtkastSchedule = new SlettUtkastSchedule(
            leaderElectionClient, veilarboppfolgingClient,
            aktorOppslagClient, vedtakService, vedtaksstotteRepository
    );

    @Test
    public void slettGamleUtkast__skal_slette_gammelt_utkast() {
        Vedtak gammeltUtkast = new Vedtak();
        gammeltUtkast.setAktorId("22222222");
        gammeltUtkast.setVedtakStatus(VedtakStatus.UTKAST);
        gammeltUtkast.setUtkastSistOppdatert(LocalDateTime.now().minusDays(30));

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(ZonedDateTime.now().minusDays(50));
        oppfolgingPeriode.setSluttDato(ZonedDateTime.now().minusDays(30));

        when(vedtaksstotteRepository.hentUtkastEldreEnn(any())).thenReturn(List.of(gammeltUtkast));
        when(vedtaksstotteRepository.hentGjeldendeVedtak(any())).thenReturn(null);
        when(aktorOppslagClient.hentFnr(any(AktorId.class))).thenReturn(Fnr.of("test"));
        when(veilarboppfolgingClient.hentOppfolgingsperioder(any())).thenReturn(List.of(oppfolgingPeriode));

        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, times(1)).slettUtkast(any());
    }

    @Test
    public void slettGamleUtkast__skal_ikke_slette_utkast_innenfor_28_dager() {
        Vedtak gammeltUtkast = new Vedtak();
        gammeltUtkast.setAktorId("22222222");
        gammeltUtkast.setVedtakStatus(VedtakStatus.UTKAST);
        gammeltUtkast.setUtkastSistOppdatert(LocalDateTime.now().minusDays(30));

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(ZonedDateTime.now().minusDays(20));
        oppfolgingPeriode.setSluttDato(ZonedDateTime.now().minusDays(18));

        when(vedtaksstotteRepository.hentUtkastEldreEnn(any())).thenReturn(List.of(gammeltUtkast));
        when(vedtaksstotteRepository.hentGjeldendeVedtak(any())).thenReturn(null);
        when(aktorOppslagClient.hentFnr(any(AktorId.class))).thenReturn(Fnr.of("test"));
        when(veilarboppfolgingClient.hentOppfolgingsperioder(any())).thenReturn(List.of(oppfolgingPeriode));

        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, never()).slettUtkast(any());
    }

}
