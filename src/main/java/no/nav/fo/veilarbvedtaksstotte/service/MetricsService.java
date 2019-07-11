package no.nav.fo.veilarbvedtaksstotte.service;

import io.vavr.control.Try;
import no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.utils.OppfolgingUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Service
public class MetricsService {

    private OppfolgingClient oppfolgingClient;

    @Inject
    public MetricsService (OppfolgingClient oppfolgingClient)  {
        this.oppfolgingClient = oppfolgingClient;
    }

    private static Event createMetricEvent(String tagName) {
        return MetricsFactory.createEvent(APPLICATION_NAME + ".metrikker." + tagName);
    }

    private static long localDateTimeToMillis(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public void rapporterVedtakSendt(Vedtak vedtak) {
        Event event = createMetricEvent("vedtak-sendt");
        long utkastOpprettetMillis = localDateTimeToMillis(vedtak.getUtkastOpprettet());
        long secondsUsed = (System.currentTimeMillis() - utkastOpprettetMillis) / 1000;

        event.addFieldToReport("sekunderBrukt", secondsUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());

        if (vedtak.getHovedmal() != null) {
            event.addFieldToReport("hovedmal", vedtak.getHovedmal());
        }

        event.report();
    }


    public void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, String fnr) {
        boolean erSykmeldtMedArbeidsgiver = Try.of(() -> oppfolgingClient.hentServicegruppe(fnr))
                .map(OppfolgingUtils::erSykmeldtUtenArbeidsgiver)
                .getOrElse(false);

        if (erSykmeldtMedArbeidsgiver) {
            Try.of(() -> oppfolgingClient.hentOppfolgingsPerioder(fnr))
                    .map(OppfolgingUtils::getOppfolgingStartDato)
                    .map(startDato ->
                            Optional.ofNullable(startDato)
                                    .map(dato -> {
                                        LocalDate vedtakSendtDato = LocalDate.now();
                                        Long diff = Duration.between(vedtakSendtDato.atStartOfDay(), dato.atStartOfDay()).toDays();
                                        Event event = createMetricEvent("sykmeldt-uten-arbeidsgiver-vedtak-sendt");
                                        event.addFieldToReport("dagerBrukt", diff);
                                        event.addFieldToReport("oppfolgingStartDato", dato.toString());
                                        event.addFieldToReport("vedtakSendtDato", vedtakSendtDato.toString());
                                        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());
                                        event.report();
                                        return true;
                                    })
                    );
        }

    }

    public void rapporterUtkastSlettet() {
        createMetricEvent("utkast-slettet").report();
    }

}
