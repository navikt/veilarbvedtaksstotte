package no.nav.veilarbvedtaksstotte.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class SlettUtkastSchedule {

    private final static String EVERY_DAY_AT_1 = "0 1 * * *";

    private final static int DAGER_FOR_SLETT_UTKAST = 28;

    private final LeaderElectionClient leaderElectionClient;

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    private final AktorregisterClient aktorregisterClient;

    private final VedtakService vedtakService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Scheduled(cron = EVERY_DAY_AT_1)
    public void slettGamleUtkast() {
        if (leaderElectionClient.isLeader()) {
            log.info("Starter jobb for å slette gamle utkast");
            startSlettingAvGamleUtkast();
        }
    }

    private void startSlettingAvGamleUtkast() {
        ZonedDateTime slettVedtakEtter = ZonedDateTime.now().minusDays(DAGER_FOR_SLETT_UTKAST);
        List<Vedtak> gamleUtkast = vedtaksstotteRepository.hentUtkastEldreEnn(slettVedtakEtter);

        // Hvis bruker ikke har et gjeldende vedtak så er de ikke under oppfølging
        List<Vedtak> gamleUtkastUtenforOppfolging = gamleUtkast.stream()
                .filter(u -> vedtaksstotteRepository.hentGjeldendeVedtak(u.getAktorId()) == null)
                .collect(Collectors.toList());

        gamleUtkastUtenforOppfolging.forEach(utkast -> {
            Fnr fnr = aktorregisterClient.hentFnr(AktorId.of(utkast.getAktorId()));
            OppfolgingDTO oppfolging = veilarboppfolgingClient.hentOppfolgingData(fnr.get());
            Optional<OppfolgingPeriodeDTO> maybeSistePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolging.getOppfolgingsPerioder());

            maybeSistePeriode.ifPresent(sistePeriode -> {
                if (sistePeriode.sluttDato != null && slettVedtakEtter.isAfter(sistePeriode.sluttDato.atZone(ZoneId.systemDefault()))) {
                    vedtakService.slettUtkast(utkast);
                }
            });
        });
    }

}
