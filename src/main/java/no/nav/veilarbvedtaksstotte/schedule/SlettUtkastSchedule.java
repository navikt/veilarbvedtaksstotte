package no.nav.veilarbvedtaksstotte.schedule;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class SlettUtkastSchedule {

    private final static String EVERY_DAY_AT_01 = "0 0 1 * * *"; // 01:00

    private final static int DAGER_FOR_SLETT_UTKAST = 28;

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    private final AktorOppslagClient aktorOppslagClient;

    private final VedtakService vedtakService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Bean
    Task<Void> startSlettingAvGamleUtkast() {
        return Tasks.recurring("slett_gamle_utkast", Schedules.daily(LocalTime.of(1, 0)))
                .execute((instance, ctx) -> JobRunner.run("slett_gamle_utkast", this::slettGamleUtkast));
    }

    void slettGamleUtkast() {
        LocalDateTime slettVedtakEtter = LocalDateTime.now().minusDays(DAGER_FOR_SLETT_UTKAST);
        List<Vedtak> gamleUtkast = vedtaksstotteRepository.hentUtkastEldreEnn(slettVedtakEtter);

        log.info("Utkast eldre enn {} som kanskje skal slettes: {}", slettVedtakEtter, gamleUtkast.size());

        // Hvis bruker har et gjeldende vedtak så er de fortsatt under oppfølging og vi trenger ikke å slette utkastet
        List<Vedtak> gamleUtkastUtenforOppfolging = gamleUtkast.stream()
                .filter(u -> vedtaksstotteRepository.hentGjeldendeVedtak(u.getAktorId()) == null)
                .collect(Collectors.toList());

        log.info("Utkast for bruker som kanskje er utenfor oppfølging: {}", gamleUtkastUtenforOppfolging.size());

        gamleUtkastUtenforOppfolging.forEach(utkast -> {
            try {
                Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(utkast.getAktorId()));
                List<OppfolgingPeriodeDTO> oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingsperioder(fnr.get());
                Optional<OppfolgingPeriodeDTO> maybeSistePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingsperioder);

                maybeSistePeriode.ifPresent(sistePeriode -> {
                    if (sistePeriode.sluttDato != null && slettVedtakEtter.isAfter(toLocalDateTime(sistePeriode.sluttDato))) {
                        log.info("Sletter utkast automatisk. aktorId={}", utkast.getAktorId());
                        vedtakService.slettUtkast(utkast);
                    }
                });
            } catch(Exception e) {
                log.error(format("Automatisk sletting av utkast for aktorId=%s feilet", utkast.getAktorId()), e);
            }
        });
    }

}
