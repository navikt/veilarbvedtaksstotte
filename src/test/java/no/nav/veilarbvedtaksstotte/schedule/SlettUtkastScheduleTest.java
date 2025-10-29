package no.nav.veilarbvedtaksstotte.schedule;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.UTKAST;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.randomNumeric;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlettUtkastScheduleTest extends DatabaseTest {

    private final LeaderElectionClient leaderElectionClient = mock(LeaderElectionClient.class);

    private final VeilarboppfolgingClient veilarboppfolgingClient = mock(VeilarboppfolgingClient.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final VedtakService vedtakService = mock(VedtakService.class);

    static private VedtaksstotteRepository vedtaksstotteRepository;

    private final SlettUtkastSchedule slettUtkastSchedule = new SlettUtkastSchedule(
            leaderElectionClient, veilarboppfolgingClient,
            aktorOppslagClient, vedtakService, vedtaksstotteRepository
    );

    @BeforeAll
    public static void setup() {
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
    }

    @Test
    public void slettGamleUtkast__skal_slette_gammelt_utkast() {
        // skal slettes
        AktorId aktorId = AktorId.of(randomNumeric(10));
        Fnr fnr = Fnr.of(randomNumeric(11));
        insertUtkast(aktorId, LocalDateTime.now().minusDays(30));
        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(ZonedDateTime.now().minusDays(50));
        oppfolgingPeriode.setSluttDato(ZonedDateTime.now().minusDays(30));
        when(aktorOppslagClient.hentFnr(aktorId)).thenReturn(fnr);
        when(veilarboppfolgingClient.hentOppfolgingsperioder(fnr)).thenReturn(List.of(oppfolgingPeriode));

        // skal ikke slettes
        AktorId aktorId2 = AktorId.of(randomNumeric(10));
        Fnr fnr2 = Fnr.of(randomNumeric(11));
        insertUtkast(aktorId2, LocalDateTime.now().minusDays(30));
        OppfolgingPeriodeDTO oppfolgingPeriode2 = new OppfolgingPeriodeDTO();
        oppfolgingPeriode2.setStartDato(ZonedDateTime.now().minusDays(50));
        oppfolgingPeriode2.setSluttDato(ZonedDateTime.now().minusDays(27));
        when(aktorOppslagClient.hentFnr(aktorId2)).thenReturn(fnr2);
        when(veilarboppfolgingClient.hentOppfolgingsperioder(fnr2)).thenReturn(List.of(oppfolgingPeriode2));


        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, times(1)).slettUtkast(argThat(utkast -> utkast.getAktorId().equals(aktorId.get())), any());
    }

    @Test
    public void slettGamleUtkast__skal_ikke_slette_utkast_innenfor_28_dager() {
        AktorId aktorId = AktorId.of(randomNumeric(10));
        Fnr fnr = Fnr.of(randomNumeric(11));

        insertUtkast(aktorId, LocalDateTime.now().minusDays(30));

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.setStartDato(ZonedDateTime.now().minusDays(20));
        oppfolgingPeriode.setSluttDato(ZonedDateTime.now().minusDays(18));

        when(aktorOppslagClient.hentFnr(aktorId)).thenReturn(Fnr.of("test"));
        when(veilarboppfolgingClient.hentOppfolgingsperioder(fnr)).thenReturn(List.of(oppfolgingPeriode));

        slettUtkastSchedule.slettGamleUtkast();

        verify(vedtakService, never()).slettUtkast(any(), any());
    }

    private void insertUtkast(AktorId aktorId, LocalDateTime utkastSistOppdatert) {
        String sql =
                "INSERT INTO VEDTAK(AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, UTKAST_SIST_OPPDATERT)"
                        + " values(?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, aktorId.get(), TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID, UTKAST.name(), utkastSistOppdatert);
    }
}
